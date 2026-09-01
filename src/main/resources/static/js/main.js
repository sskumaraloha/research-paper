/* ============================================================
   Store Management System - global scripts
   ============================================================ */

document.addEventListener('DOMContentLoaded', function () {

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
