package com.mip.assistant.service;

import com.mip.dictionary.entity.FailureMode;
import com.mip.machine.entity.Machine;
import com.mip.part.entity.SparePart;

/** The classified intent of a question, with the entities extracted from it. */
public record RoutedIntent(
        Intent intent,
        Machine machine,
        FailureMode failureMode,
        SparePart part
) {
    public static RoutedIntent of(Intent intent) {
        return new RoutedIntent(intent, null, null, null);
    }
}
