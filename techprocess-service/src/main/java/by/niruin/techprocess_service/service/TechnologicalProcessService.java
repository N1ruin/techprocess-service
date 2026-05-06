package by.niruin.techprocess_service.service;

import by.niruin.techprocess_service.domain.TechnologicalProcess;
import by.niruin.techprocess_service.domain.TechnologicalProcessOrganizationType;
import by.niruin.techprocess_service.domain.TechnologicalProcessStatus;
import by.niruin.techprocess_service.exception.EntityAlreadyExistedException;
import by.niruin.techprocess_service.exception.EntityNotFoundException;
import by.niruin.techprocess_service.repository.TechnologicalProcessRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
public class TechnologicalProcessService {
    private final TechnologicalProcessRepository repository;

    public TechnologicalProcessService(TechnologicalProcessRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TechnologicalProcess save(TechnologicalProcess technologicalProcess) {
        repository.findByPartNumberAndTypeAndWorkshopCode(technologicalProcess.getPartNumber(),
                        technologicalProcess.getType(), technologicalProcess.getWorkshopCode())
                .ifPresent(existed -> {
                    throw new EntityAlreadyExistedException("TP for this part/workshop already exists");
                });

        repository.findByArchiveNumber(technologicalProcess.getArchiveNumber())
                .ifPresent(existed -> {
                    throw new EntityAlreadyExistedException("Archive number already taken");
                });

        technologicalProcess.setCreated(LocalDateTime.now());
        technologicalProcess.setUpdated(LocalDateTime.now());
        technologicalProcess.setRevision(0);
        technologicalProcess.setStatus(TechnologicalProcessStatus.IN_DEVELOPMENT);

        return repository.save(technologicalProcess);
    }

    @Transactional
    public TechnologicalProcess update(TechnologicalProcess technologicalProcess) {
        TechnologicalProcess existing =
                repository.findFirstByArchiveNumberOrderByRevisionDesc(technologicalProcess.getArchiveNumber())
                        .orElseThrow(() -> new EntityNotFoundException("TP NOT FOUND"));

        if (existing.getStatus() == TechnologicalProcessStatus.PRODUCTION) {
            throw new IllegalStateException("Cannot update a production-ready process. Use revision instead.");// todo сделать кастомное исключение
        }

        technologicalProcess.setId(existing.getId());
        technologicalProcess.setCreated(existing.getCreated());
        technologicalProcess.setUpdated(LocalDateTime.now());
        technologicalProcess.setRevision(existing.getRevision());

        return repository.save(technologicalProcess);
    }

    public TechnologicalProcess createCorrection(String archiveNumber) {
        var technologicalProcess = repository.findFirstByArchiveNumberOrderByRevisionDesc(archiveNumber)
                .orElseThrow(() -> new EntityNotFoundException("ТП не найден"));

        if (technologicalProcess.getStatus() != TechnologicalProcessStatus.SET_UP) {
            throw new IllegalStateException("Корректировка разрешена только в стадии наладки");
        }

        TechnologicalProcess newRevision = new TechnologicalProcess();

        newRevision.setPartNumber(technologicalProcess.getPartNumber());
        newRevision.setName(technologicalProcess.getName());
        newRevision.setArchiveNumber(technologicalProcess.getArchiveNumber());
        newRevision.setWorkshopCode(technologicalProcess.getWorkshopCode());
        newRevision.setType(technologicalProcess.getType());

        newRevision.setRevision(technologicalProcess.getRevision() + 1);
//todo аннулировать старые ревизии при обновлении

        newRevision.setTechnologicalOperations(new ArrayList<>(technologicalProcess.getTechnologicalOperations()));
        newRevision.setSketches(new ArrayList<>(technologicalProcess.getSketches()));

        newRevision.setStatus(TechnologicalProcessStatus.SET_UP);
        newRevision.setCreated(LocalDateTime.now());
        return repository.save(newRevision);
    }

    public void cancel(String partNumber, TechnologicalProcessOrganizationType type, String workshopCode) {
        repository.findByPartNumberAndTypeAndWorkshopCode(partNumber, type, workshopCode)
                .ifPresent(existed -> {
                    if (existed.getStatus() == TechnologicalProcessStatus.CANCELLED
                            || existed.getStatus() == TechnologicalProcessStatus.PRODUCTION) {
                        throw new RuntimeException(); // todo кастомное исключение
                    }
//todo transaction outbox для кафки и сохранения
                    existed.setStatus(TechnologicalProcessStatus.CANCELLED);

                    repository.save(existed);
                });
    }
}
