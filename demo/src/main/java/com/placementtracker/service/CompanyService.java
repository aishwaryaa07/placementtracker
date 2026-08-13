package com.placementtracker.service;

import com.placementtracker.dto.CompanyRequest;
import com.placementtracker.dto.CompanyResponse;
import com.placementtracker.exception.ResourceNotFoundException;
import com.placementtracker.model.Company;
import com.placementtracker.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyResponse create(CompanyRequest request) {
        Company company = new Company();
        applyRequest(company, request);
        return new CompanyResponse(companyRepository.save(company));
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> findAll() {
        return companyRepository.findAll().stream().map(CompanyResponse::new).toList();
    }

    @Transactional(readOnly = true)
    public CompanyResponse findById(Long id) {
        return new CompanyResponse(getCompanyOrThrow(id));
    }

    public CompanyResponse update(Long id, CompanyRequest request) {
        Company company = getCompanyOrThrow(id);
        applyRequest(company, request);
        return new CompanyResponse(companyRepository.save(company));
    }

    public void delete(Long id) {
        if (!companyRepository.existsById(id)) {
            throw new ResourceNotFoundException("No company found with id " + id);
        }
        companyRepository.deleteById(id);
    }

    private Company getCompanyOrThrow(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No company found with id " + id));
    }

    private void applyRequest(Company company, CompanyRequest request) {
        company.setName(request.getName());
        company.setWebsite(request.getWebsite());
        company.setDescription(request.getDescription());
        company.setContactEmail(request.getContactEmail());
    }
}
