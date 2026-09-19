package com.mip.entry.service;

import com.mip.entry.dto.EntryConversationResponse;
import com.mip.entry.dto.EntryDraftResponse;
import com.mip.entry.dto.EntryMessageRequest;
import com.mip.entry.dto.EntryMessageResponse;
import com.mip.entry.dto.StartConversationRequest;
import com.mip.entry.dto.WhatsAppReplyResponse;
import com.mip.entry.entity.EntryConversation;
import com.mip.entry.entity.EntryMessage;
import com.mip.entry.repository.EntryConversationRepository;
import com.mip.entry.repository.EntryMessageRepository;
import com.mip.exception.BusinessRuleViolationException;
import com.mip.exception.ForbiddenException;
import com.mip.exception.InvalidStateTransitionException;
import com.mip.exception.ResourceNotFoundException;
import com.mip.exception.UnauthorizedException;
import com.mip.part.entity.SparePart;
import com.mip.plant.entity.Plant;
import com.mip.plant.service.PlantService;
import com.mip.record.dto.CreateRecordRequest;
import com.mip.record.entity.MaintenanceRecord;
import com.mip.record.service.MaintenanceRecordService;
import com.mip.security.MipUserDetails;
import com.mip.user.entity.RoleName;
import com.mip.user.entity.User;
import com.mip.user.repository.UserRepository;
import com.mip.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The guided record-entry agent: each user message is mined for fields, the draft is
 * shown back, and once everything required is present the user confirms and a real
 * maintenance record is created.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EntryAgentService {

    private static final Set<String> CONFIRM_WORDS = Set.of("confirm", "yes", "y", "ok", "save", "done");

    private final EntryConversationRepository conversationRepository;
    private final EntryMessageRepository messageRepository;
    private final EntryExtractionService extractionService;
    private final MaintenanceRecordService recordService;
    private final PlantService plantService;
    private final UserService userService;
    private final UserRepository userRepository;

    @Transactional
    public EntryConversationResponse startConversation(StartConversationRequest request,
                                                       MipUserDetails principal) {
        Plant plant = plantService.requireAccessiblePlant(request.plantId(), principal);
        User user = userService.getUser(principal.getId());
        EntryConversation conversation = conversationRepository.save(new EntryConversation(user, plant));

        if (request.message() != null && !request.message().isBlank()) {
            processUserMessage(conversation, request.message());
        } else {
            reply(conversation, "Hi! Tell me what happened: which machine, what went wrong, "
                    + "when, and how long it was down.");
        }
        return toResponse(conversation);
    }

    @Transactional
    public EntryConversationResponse handleMessage(Long conversationId, EntryMessageRequest request,
                                                   MipUserDetails principal) {
        EntryConversation conversation = requireOwnConversation(conversationId, principal);
        if (conversation.getStatus() == EntryConversation.ConversationStatus.CONFIRMED
                || conversation.getStatus() == EntryConversation.ConversationStatus.CANCELLED) {
            throw new InvalidStateTransitionException(
                    "This conversation is closed; start a new one to log another record");
        }
        conversation.setStatus(EntryConversation.ConversationStatus.COLLECTING);
        processUserMessage(conversation, request.message());
        return toResponse(conversation);
    }

    @Transactional
    public EntryConversationResponse confirm(Long conversationId, MipUserDetails principal) {
        EntryConversation conversation = requireOwnConversation(conversationId, principal);
        if (conversation.getStatus() != EntryConversation.ConversationStatus.AWAITING_CONFIRMATION) {
            throw new InvalidStateTransitionException(
                    "There is nothing to confirm yet; the draft is still incomplete");
        }
        CreateRecordRequest request = new CreateRecordRequest(
                conversation.getPlant().getId(),
                conversation.getMachine().getId(),
                conversation.getRecordDate(),
                conversation.getDowntimeMinutes(),
                conversation.getDescription(),
                conversation.getActionTaken(),
                conversation.getUser().getFullName(),
                conversation.getFailureMode() == null ? null : conversation.getFailureMode().getId(),
                null,
                conversation.getPartsText() == null ? null
                        : Arrays.stream(conversation.getPartsText().split(","))
                                .map(String::trim).filter(s -> !s.isEmpty()).toList());
        MaintenanceRecord record = recordService.createRecordFromAgent(request, conversation.getUser());
        conversation.setResultingRecord(record);
        conversation.setStatus(EntryConversation.ConversationStatus.CONFIRMED);
        reply(conversation, "Done - record #" + record.getId() + " is saved for "
                + conversation.getMachine().getName() + ". Thanks!");
        log.info("Entry conversation {} confirmed into record {}", conversationId, record.getId());
        return toResponse(conversation);
    }

    @Transactional
    public EntryConversationResponse requestEdit(Long conversationId, MipUserDetails principal) {
        EntryConversation conversation = requireOwnConversation(conversationId, principal);
        if (conversation.getStatus() != EntryConversation.ConversationStatus.AWAITING_CONFIRMATION) {
            throw new InvalidStateTransitionException("Only a draft awaiting confirmation can be edited");
        }
        conversation.setStatus(EntryConversation.ConversationStatus.COLLECTING);
        reply(conversation, "Sure - tell me what to change (for example \"downtime was 3 hours\" "
                + "or \"it was yesterday, not today\").");
        return toResponse(conversation);
    }

    @Transactional(readOnly = true)
    public EntryConversationResponse getConversation(Long conversationId, MipUserDetails principal) {
        return toResponse(requireOwnConversation(conversationId, principal));
    }

    /**
     * Inbound WhatsApp message relayed by a gateway. The sender is identified by phone
     * number; the message continues their open WhatsApp conversation (a confirm word
     * saves an awaiting draft) or starts a new one on their first assigned plant.
     */
    @Transactional
    public WhatsAppReplyResponse handleWhatsAppInbound(String from, String text) {
        User user = userRepository.findByPhoneNumber(from.trim())
                .filter(User::isActive)
                .orElseThrow(() -> new UnauthorizedException("Unknown sender"));
        if (user.getRole() == RoleName.VIEWER) {
            throw new ForbiddenException("This account cannot log maintenance records");
        }
        MipUserDetails principal = new MipUserDetails(user);
        EntryConversation open = conversationRepository
                .findTopByUserIdAndChannelOrderByIdDesc(user.getId(),
                        EntryConversation.Channel.WHATSAPP)
                .filter(c -> c.getStatus() == EntryConversation.ConversationStatus.COLLECTING
                        || c.getStatus() == EntryConversation.ConversationStatus.AWAITING_CONFIRMATION)
                .orElse(null);

        EntryConversationResponse response;
        if (open != null
                && open.getStatus() == EntryConversation.ConversationStatus.AWAITING_CONFIRMATION
                && CONFIRM_WORDS.contains(text.trim().toLowerCase(Locale.ROOT))) {
            response = confirm(open.getId(), principal);
        } else if (open != null) {
            response = handleMessage(open.getId(), new EntryMessageRequest(text), principal);
        } else {
            Plant plant = user.getPlants().stream()
                    .min(Comparator.comparing(Plant::getId))
                    .orElseThrow(() -> new BusinessRuleViolationException(
                            "No plant is assigned to this account"));
            EntryConversation conversation = conversationRepository.save(
                    new EntryConversation(user, plant, EntryConversation.Channel.WHATSAPP));
            processUserMessage(conversation, text);
            response = toResponse(conversation);
        }
        return new WhatsAppReplyResponse(response.id(), response.status(),
                lastAssistantMessage(response));
    }

    private String lastAssistantMessage(EntryConversationResponse response) {
        for (int i = response.messages().size() - 1; i >= 0; i--) {
            if ("ASSISTANT".equals(response.messages().get(i).sender())) {
                return response.messages().get(i).content();
            }
        }
        return "Message received.";
    }

    // --- internals ---

    private void processUserMessage(EntryConversation conversation, String message) {
        messageRepository.save(new EntryMessage(conversation, EntryMessage.Sender.USER, message));
        EntryExtractionService.ExtractedFields extracted =
                extractionService.extract(conversation.getPlant().getId(), message);

        if (extracted.machine() != null) {
            conversation.setMachine(extracted.machine().machine());
            conversation.setMachineText(message.length() > 200 ? message.substring(0, 200) : message);
        }
        if (extracted.date() != null) {
            conversation.setRecordDate(extracted.date());
        }
        if (extracted.downtimeMinutes() != null) {
            conversation.setDowntimeMinutes(extracted.downtimeMinutes());
        }
        if (extracted.failureMode() != null) {
            conversation.setFailureMode(extracted.failureMode());
        }
        if (!extracted.parts().isEmpty()) {
            conversation.setPartsText(extracted.parts().stream()
                    .map(SparePart::getName).distinct().reduce((a, b) -> a + "," + b).orElse(null));
        }
        if (conversation.getDescription() == null) {
            conversation.setDescription(message.trim());
        } else if (message.trim().length() > 15
                && !message.trim().equalsIgnoreCase(conversation.getDescription())) {
            String merged = conversation.getDescription() + " | " + message.trim();
            conversation.setDescription(merged.length() > 2000 ? merged.substring(0, 2000) : merged);
        }

        List<String> missing = missingFields(conversation);
        if (missing.isEmpty()) {
            conversation.setStatus(EntryConversation.ConversationStatus.AWAITING_CONFIRMATION);
            reply(conversation, summary(conversation)
                    + "\nShall I save it? Confirm, or tell me what to change.");
        } else {
            reply(conversation, "Got it. I still need: " + String.join(", ", missing) + ".");
        }
    }

    private List<String> missingFields(EntryConversation conversation) {
        List<String> missing = new ArrayList<>();
        if (conversation.getMachine() == null) {
            missing.add("the machine (name or code)");
        }
        if (conversation.getRecordDate() == null) {
            missing.add("when it happened (e.g. today, yesterday, 2026-09-15)");
        }
        if (conversation.getDowntimeMinutes() == null) {
            missing.add("how long it was down (e.g. 45 min, 2 hours)");
        }
        if (conversation.getDescription() == null) {
            missing.add("what happened");
        }
        return missing;
    }

    private String summary(EntryConversation conversation) {
        StringBuilder text = new StringBuilder("Here is what I have:\n");
        text.append("- Machine: ").append(conversation.getMachine().getName()).append("\n");
        text.append("- Date: ").append(conversation.getRecordDate()).append("\n");
        text.append("- Downtime: ").append(conversation.getDowntimeMinutes()).append(" min\n");
        if (conversation.getFailureMode() != null) {
            text.append("- Failure mode: ").append(conversation.getFailureMode().getName()).append("\n");
        }
        if (conversation.getPartsText() != null) {
            text.append("- Parts: ").append(conversation.getPartsText()).append("\n");
        }
        text.append("- Description: ").append(conversation.getDescription());
        return text.toString();
    }

    private void reply(EntryConversation conversation, String content) {
        messageRepository.save(new EntryMessage(conversation, EntryMessage.Sender.ASSISTANT, content));
    }

    private EntryConversation requireOwnConversation(Long conversationId, MipUserDetails principal) {
        return conversationRepository.findByIdAndUserId(conversationId, principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId));
    }

    private EntryConversationResponse toResponse(EntryConversation conversation) {
        List<EntryMessageResponse> messages = messageRepository
                .findByConversationIdOrderByCreatedAtAscIdAsc(conversation.getId()).stream()
                .map(m -> new EntryMessageResponse(m.getSender().name(), m.getContent(), m.getCreatedAt()))
                .toList();
        EntryDraftResponse draft = new EntryDraftResponse(
                conversation.getMachine() == null ? null : conversation.getMachine().getId(),
                conversation.getMachine() == null ? null : conversation.getMachine().getName(),
                conversation.getMachineText(),
                conversation.getRecordDate(), conversation.getDowntimeMinutes(),
                conversation.getDescription(), conversation.getActionTaken(),
                conversation.getFailureMode() == null ? null : conversation.getFailureMode().getId(),
                conversation.getFailureMode() == null ? null : conversation.getFailureMode().getName(),
                conversation.getPartsText(), missingFields(conversation));
        return new EntryConversationResponse(conversation.getId(), conversation.getPlant().getId(),
                conversation.getStatus().name(), draft, messages,
                conversation.getResultingRecord() == null ? null
                        : conversation.getResultingRecord().getId());
    }
}
