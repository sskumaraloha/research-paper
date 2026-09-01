/* ============================================================
   Store Management System - global scripts
   ============================================================ */

document.addEventListener('DOMContentLoaded', function () {

    // Placeholder actions for features not implemented yet: any element
    // with data-coming-soon="<Feature>" shows a toast instead of acting.
    document.body.addEventListener('click', function (event) {
        var trigger = event.target.closest('[data-coming-soon]');
        if (!trigger) {
            return;
        }
        event.preventDefault();
        showToast(trigger.dataset.comingSoon + ' is coming soon!');
    });

    function showToast(message) {
        var container = document.getElementById('appToastContainer');
        if (!container) {
            container = document.createElement('div');
            container.id = 'appToastContainer';
            container.className = 'toast-container position-fixed bottom-0 end-0 p-3';
            document.body.appendChild(container);
        }

        var toast = document.createElement('div');
        toast.className = 'toast align-items-center text-bg-dark border-0';
        toast.setAttribute('role', 'status');
        toast.innerHTML =
            '<div class="d-flex">' +
            '<div class="toast-body"></div>' +
            '<button type="button" class="btn-close btn-close-white me-2 m-auto" ' +
            'data-bs-dismiss="toast" aria-label="Close"></button></div>';
        toast.querySelector('.toast-body').textContent = message;
        container.appendChild(toast);

        var instance = window.bootstrap
            ? new bootstrap.Toast(toast, {delay: 2500})
            : null;
        if (instance) {
            toast.addEventListener('hidden.bs.toast', function () { toast.remove(); });
            instance.show();
        } else {
            toast.classList.add('show');
            setTimeout(function () { toast.remove(); }, 2500);
        }
    }

    // Bootstrap client-side validation for all forms marked .needs-validation.
    document.querySelectorAll('form.needs-validation').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            syncMatchFields(form);
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
            }
            form.classList.add('was-validated');
        });
    });

    // Live "must match" validation, e.g. confirm-password inputs declaring
    // data-match-target="<id of the field they must match>".
    document.querySelectorAll('input[data-match-target]').forEach(function (input) {
        var target = document.getElementById(input.dataset.matchTarget);
        if (!target) {
            return;
        }
        var validate = function () {
            input.setCustomValidity(
                input.value === target.value ? '' : 'Values do not match');
        };
        input.addEventListener('input', validate);
        target.addEventListener('input', validate);
    });

    function syncMatchFields(form) {
        form.querySelectorAll('input[data-match-target]').forEach(function (input) {
            var target = document.getElementById(input.dataset.matchTarget);
            if (target) {
                input.setCustomValidity(
                    input.value === target.value ? '' : 'Values do not match');
            }
        });
    }
});
