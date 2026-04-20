package com.yourcompany.credittracker.service;

import com.yourcompany.credittracker.dto.ContactRequest;
import com.yourcompany.credittracker.dto.CustomerRequest;
import com.yourcompany.credittracker.exception.NotFoundException;
import com.yourcompany.credittracker.model.Customer;
import com.yourcompany.credittracker.model.CustomerContact;
import com.yourcompany.credittracker.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public List<Customer> search(String search) {
        if (search == null || search.isBlank()) {
            return List.of();
        }
        String term = search.trim();
        return customerRepository.findTop10ByActiveTrueAndNameContainingIgnoreCaseOrActiveTrueAndPhoneContainingOrderByNameAsc(term, term);
    }

    @Transactional(readOnly = true)
    public List<Customer> allActive() {
        return customerRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Customer get(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> new NotFoundException("Customer not found"));
    }

    @Transactional
    public Customer create(CustomerRequest request) {
        ensureUniqueNamePhone(request.name(), request.phone(), null);
        Customer customer = new Customer();
        apply(customer, request);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer update(Long id, CustomerRequest request) {
        Customer customer = get(id);
        ensureUniqueNamePhone(request.name(), request.phone(), id);
        customer.getContacts().clear();
        apply(customer, request);
        return customer;
    }

    @Transactional
    public Customer deactivate(Long id) {
        Customer customer = get(id);
        customer.setActive(false);
        return customer;
    }

    private void apply(Customer customer, CustomerRequest request) {
        customer.setName(request.name().trim());
        customer.setNotes(request.notes());
        customer.setEmail(request.email());
        customer.setPhone(request.phone().trim());
        for (ContactRequest contactRequest : request.safeContacts()) {
            if (contactRequest.name() == null || contactRequest.name().isBlank()) {
                continue;
            }
            CustomerContact contact = new CustomerContact();
            contact.setCustomer(customer);
            contact.setName(contactRequest.name().trim());
            contact.setEmail(contactRequest.email());
            contact.setPhone(contactRequest.phone());
            contact.setRelationship(contactRequest.relationship());
            customer.getContacts().add(contact);
        }
    }

    private void ensureUniqueNamePhone(String name, String phone, Long currentCustomerId) {
        String normalizedName = name == null ? "" : name.trim();
        String normalizedPhone = phone == null ? "" : phone.trim();
        if (normalizedPhone.isBlank()) {
            throw new IllegalArgumentException("Phone is required");
        }
        customerRepository.findByNameIgnoreCaseAndPhone(normalizedName, normalizedPhone).ifPresent(existing -> {
            if (currentCustomerId == null || !existing.getId().equals(currentCustomerId)) {
                throw new IllegalArgumentException("Customer name and phone combination already exists");
            }
        });
    }
}
