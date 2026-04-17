function addContactRow() {
    const container = document.querySelector('[data-contacts]');
    if (!container) return;
    const row = document.createElement('div');
    row.className = 'inline card';
    row.innerHTML = `
        <div><label>Name</label><input name="contactName" required></div>
        <div><label>Email</label><input name="contactEmail" type="email"></div>
        <div><label>Phone</label><input name="contactPhone"></div>
        <div><label>Relationship</label><input name="contactRelationship"></div>
        <button type="button" class="red small" onclick="this.closest('.inline').remove()">Remove</button>
    `;
    container.appendChild(row);
}

function syncCreditControls() {
    const productSelect = document.querySelector('[data-credit-product]');
    if (productSelect) {
        const selected = productSelect.selectedOptions[0];
        const balance = selected ? Number.parseInt(selected.dataset.balance || '0', 10) : 0;
        const maxDeduction = Math.max(balance, 0);
        const unitLabel = selected ? selected.dataset.unitLabel : 'credits';
        document.querySelectorAll('[data-selected-product-id]').forEach((input) => {
            input.value = productSelect.value;
        });
        document.querySelectorAll('[data-current-price]').forEach((node) => {
            node.textContent = selected ? selected.dataset.price : '';
        });
        document.querySelectorAll('[data-consumption-units]').forEach((input) => {
            input.max = String(maxDeduction);
            input.disabled = maxDeduction < 1;
            if (Number.parseInt(input.value || '0', 10) > maxDeduction) {
                input.value = '';
            }
        });
        document.querySelectorAll('[data-adjustment-units]').forEach((input) => {
            input.min = maxDeduction > 0 ? String(-maxDeduction) : '1';
            if (Number.parseInt(input.value || '0', 10) < Number.parseInt(input.min, 10)) {
                input.value = '';
            }
        });
        document.querySelectorAll('[data-balance-limit]').forEach((node) => {
            node.textContent = maxDeduction > 0
                ? `You can consume up to ${maxDeduction} ${unitLabel}.`
                : `No consumption allowed. Balance is already zero.`;
        });
        document.querySelectorAll('[data-adjustment-limit]').forEach((node) => {
            node.textContent = maxDeduction > 0
                ? `Negative adjustments can subtract up to ${maxDeduction} ${unitLabel}.`
                : `Negative adjustments are not allowed. Balance is already zero.`;
        });
    }

    const selectedAction = document.querySelector('input[name="creditAction"]:checked');
    if (selectedAction) {
        document.querySelectorAll('[data-action-panel]').forEach((panel) => {
            panel.classList.toggle('hidden', panel.dataset.actionPanel !== selectedAction.value);
        });
    }
}

document.addEventListener('submit', (event) => {
    const form = event.target;
    if (form.matches('[data-confirm]') && !confirm(form.dataset.confirm)) {
        event.preventDefault();
        return;
    }
    form.classList.add('loading');
});

document.addEventListener('change', (event) => {
    if (event.target.matches('[data-credit-product], input[name="creditAction"]')) {
        syncCreditControls();
    }
});

document.addEventListener('click', (event) => {
    const toggle = event.target.closest('[data-menu-toggle]');
    if (toggle) {
        const open = !document.body.classList.contains('menu-open');
        document.body.classList.toggle('menu-open', open);
        toggle.setAttribute('aria-expanded', String(open));
        toggle.setAttribute('aria-label', open ? 'Close menu' : 'Open menu');
        return;
    }

    if (document.body.classList.contains('menu-open') && !event.target.closest('.nav')) {
        document.body.classList.remove('menu-open');
        const menuToggle = document.querySelector('[data-menu-toggle]');
        if (menuToggle) {
            menuToggle.setAttribute('aria-expanded', 'false');
            menuToggle.setAttribute('aria-label', 'Open menu');
        }
    }
});

document.addEventListener('input', (event) => {
    if (!event.target.matches('[data-search-customers]')) return;
    const term = event.target.value.toLowerCase();
    document.querySelectorAll('[data-customer-option]').forEach((row) => {
        row.classList.toggle('hidden', !row.textContent.toLowerCase().includes(term));
    });
});

setTimeout(() => {
    document.querySelectorAll('.toast').forEach((toast) => toast.remove());
}, 3500);

document.addEventListener('DOMContentLoaded', syncCreditControls);
