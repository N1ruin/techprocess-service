package by.niruin.techprocess_service.service;

import by.niruin.techprocess_service.repository.TechnologicalProcessRepository;
import org.springframework.stereotype.Service;

@Service
public class TechnologicalProcessService {
    private final TechnologicalProcessRepository repository;

    public TechnologicalProcessService(TechnologicalProcessRepository repository) {
        this.repository = repository;
    }

}
