package by.niruin.techprocess_service.service;

import by.niruin.techprocess_service.domain.*;
import by.niruin.techprocess_service.exception.EntityAlreadyExistedException;
import by.niruin.techprocess_service.exception.EntityNotFoundException;
import by.niruin.techprocess_service.repository.TechnologicalProcessRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TechnologicalProcessService {
    private final TechnologicalProcessRepository repository;

    public TechnologicalProcessService(TechnologicalProcessRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TechnologicalProcess create(TechnologicalProcess technologicalProcess) {
        repository.findByArchiveNumber(technologicalProcess.getArchiveNumber())
                .ifPresent(existed -> {
                    throw new EntityAlreadyExistedException("Архивный номер уже занят");
                });

        repository.findByPartNumberAndTypeAndWorkshopCode(
                        technologicalProcess.getPartNumber(),
                        technologicalProcess.getType(),
                        technologicalProcess.getWorkshopCode())
                .ifPresent(existed -> {
                    throw new EntityAlreadyExistedException("ТП для данной детали и цеха уже существует");
                });


        technologicalProcess.setRevision(0);
        technologicalProcess.setStatus(TechnologicalProcessStatus.IN_DEVELOPMENT);

        return repository.save(technologicalProcess);
    }

    @Transactional
    public TechnologicalProcess update(TechnologicalProcess technologicalProcess) {
        var existing = repository
                .findFirstByArchiveNumberOrderByRevisionDesc(
                        technologicalProcess.getArchiveNumber())
                .orElseThrow(() -> new EntityNotFoundException("Техпроцесс не найден"));

        if (existing.getStatus() != TechnologicalProcessStatus.IN_DEVELOPMENT
                && existing.getStatus() != TechnologicalProcessStatus.IN_CORRECTION) {
            throw new IllegalStateException(
                    "Редактирование разрешено только в статусах " +
                            "'В разработке' и 'На корректировке'");
        }

        if (!existing.getArchiveNumber().equals(technologicalProcess.getArchiveNumber())) {
            throw new IllegalStateException("Нельзя изменить архивный номер");
        }

        if (existing.getStatus() == TechnologicalProcessStatus.IN_CORRECTION
                && !existing.getWorkshopCode().equals(technologicalProcess.getWorkshopCode())) {
            throw new IllegalStateException("Нельзя изменить цех в корректировке");
        }

        technologicalProcess.setId(existing.getId());
        technologicalProcess.setRevision(existing.getRevision());
        technologicalProcess.setStatus(existing.getStatus());

        return repository.save(technologicalProcess);
    }

    @Transactional
    public TechnologicalProcess createRevision(String archiveNumber) {
        var original = repository
                .findFirstByArchiveNumberOrderByRevisionDesc(archiveNumber)
                .orElseThrow(() -> new EntityNotFoundException("ТП не найден"));

        if (original.getStatus() != TechnologicalProcessStatus.SET_UP) {
            throw new IllegalStateException(
                    "Создать новую ревизию можно только для техпроцесса в статусе 'Наладочный'");
        }

        if (repository.existsByArchiveNumberAndStatus(
                archiveNumber, TechnologicalProcessStatus.IN_CORRECTION)) {
            throw new IllegalStateException(
                    "Уже есть ревизия на корректировке. Завершите или удалите её.");
        }

        var newRevision = new TechnologicalProcess();

        newRevision.setName(original.getName());
        newRevision.setPartNumber(original.getPartNumber());
        newRevision.setArchiveNumber(original.getArchiveNumber());
        newRevision.setWorkshopCode(original.getWorkshopCode());
        newRevision.setType(original.getType());
        newRevision.setTotalSheets(original.getTotalSheets());

        newRevision.setRevision(original.getRevision() + 1);
        newRevision.setStatus(TechnologicalProcessStatus.IN_CORRECTION);

        newRevision.setTechnologicalOperations(
                deepCopyOperations(original.getTechnologicalOperations()));
        newRevision.setSketches(
                deepCopySketches(original.getSketches()));

        return repository.save(newRevision);
    }

    @Transactional
    public TechnologicalProcess completeCorrection(String archiveNumber) {
        var correction = repository
                .findFirstByArchiveNumberOrderByRevisionDesc(archiveNumber)
                .orElseThrow(() -> new EntityNotFoundException("Техпроцесс не найден"));

        if (correction.getStatus() != TechnologicalProcessStatus.IN_CORRECTION) {
            throw new IllegalStateException(
                    "Завершить можно только ревизию в статусе 'На корректировке'. " +
                            "Текущий статус: " + correction.getStatus());
        }

        var previousRevision = correction.getRevision() - 1;
        repository.findByArchiveNumberAndRevision(archiveNumber, previousRevision)
                .ifPresent(previous -> {
                    previous.setStatus(TechnologicalProcessStatus.CANCELLED);
                    repository.save(previous);
                });

        correction.setStatus(TechnologicalProcessStatus.SET_UP);
        return repository.save(correction);
    }


    @Transactional
    public TechnologicalProcess toSetUp(String archiveNumber) {
        var process = repository
                .findFirstByArchiveNumberOrderByRevisionDesc(archiveNumber)
                .orElseThrow(() -> new EntityNotFoundException("Техпроцесс не найден"));

        if (process.getStatus() != TechnologicalProcessStatus.IN_DEVELOPMENT) {
            throw new IllegalStateException(
                    "Перевод в наладку возможен только из статуса 'В разработке'. " +
                            "Текущий статус: " + process.getStatus());
        }

        process.setStatus(TechnologicalProcessStatus.SET_UP);
        return repository.save(process);
    }

    @Transactional
    public void cancel(String archiveNumber) {
        var process = repository
                .findFirstByArchiveNumberOrderByRevisionDesc(archiveNumber)
                .orElseThrow(() -> new EntityNotFoundException("Техпроцесс не найден"));

        if (process.getStatus() == TechnologicalProcessStatus.IN_CORRECTION) {
            throw new IllegalStateException(
                    "Нельзя аннулировать. Последняя ревизия на корректировке. " +
                            "Завершите или удалите корректировку.");
        }

        if (process.getStatus() == TechnologicalProcessStatus.CANCELLED
                || process.getStatus() == TechnologicalProcessStatus.PRODUCTION) {
            throw new IllegalStateException(
                    "Нельзя аннулировать техпроцесс в статусе " + process.getStatus());
        }

        process.setStatus(TechnologicalProcessStatus.CANCELLED);
        repository.save(process);
    }

    @Transactional
    public void deleteRevision(String archiveNumber) {
        var revision = repository
                .findFirstByArchiveNumberOrderByRevisionDesc(archiveNumber)
                .orElseThrow(() -> new EntityNotFoundException("ТП не найден"));

        if (revision.getStatus() != TechnologicalProcessStatus.IN_CORRECTION) {
            throw new IllegalStateException(
                    "Удалить можно только ревизию в статусе 'На корректировке'. " +
                            "Текущий статус: " + revision.getStatus());
        }

        repository.delete(revision);
    }

    public TechnologicalProcess getByArchiveNumber(String archiveNumber) {
        return repository.findFirstByArchiveNumberOrderByRevisionDesc(archiveNumber)
                .orElseThrow(() -> new EntityNotFoundException("Техпроцесс не найден"));
    }

    private List<TechnologicalOperation> deepCopyOperations(List<TechnologicalOperation> originals) {
        List<TechnologicalOperation> copies = new ArrayList<>();
        for (var original : originals) {
            var copy = new TechnologicalOperation();
            copy.setNumber(original.getNumber());
            copy.setName(original.getName());
            copy.setWorkplace(original.getWorkplace());
            copy.setEquipment(original.getEquipment());
            copy.setWorkerCode(original.getWorkerCode());
            copy.setWorkerCategory(original.getWorkerCategory());
            copy.setSafetyInstructionNumber(new ArrayList<>(original.getSafetyInstructionNumber()));
            copy.setProducts(deepCopyProducts(original.getProducts()));
            copy.setMaterials(deepCopyMaterials(original.getMaterials()));
            copy.setTransitions(deepCopyTransitions(original.getTransitions()));

            copies.add(copy);
        }

        return copies;
    }

    private List<Product> deepCopyProducts(List<Product> originals) {
        List<Product> copies = new ArrayList<>();
        for (var original : originals) {
            var copy = new Product();
            copy.setPosition(original.getPosition());
            copy.setName(original.getName());
            copy.setNumber(original.getNumber());
            copy.setSupplierCode(original.getSupplierCode());
            copy.setMaterialUnit(original.getMaterialUnit());
            copy.setQuantity(original.getQuantity());

            copies.add(copy);
        }

        return copies;
    }

    private List<Material> deepCopyMaterials(List<Material> originals) {
        List<Material> copies = new ArrayList<>();
        for (var original : originals) {
            var copy = new Material();
            copy.setPosition(original.getPosition());
            copy.setName(original.getName());
            copy.setSupplierCode(original.getSupplierCode());
            copy.setUnit(original.getUnit());
            copy.setRationingUnit(original.getRationingUnit());
            copy.setConsumptionRate(original.getConsumptionRate());

            copies.add(copy);
        }

        return copies;
    }

    private List<TechnologicalTransition> deepCopyTransitions(List<TechnologicalTransition> originals) {
        List<TechnologicalTransition> copies = new ArrayList<>();
        for (var original : originals) {
            var copy = new TechnologicalTransition();
            copy.setNumber(original.getNumber());
            copy.setContent(original.getContent());
            copy.setTools(new ArrayList<>(original.getTools()));

            copies.add(copy);
        }
        return copies;
    }

    private List<SketchCard> deepCopySketches(List<SketchCard> originals) {
        List<SketchCard> copies = new ArrayList<>();
        for (var original : originals) {
            var copy = new SketchCard();
            copy.setOperationNumbers(new ArrayList<>(original.getOperationNumbers()));
            copy.setFileStorageId(original.getFileStorageId());

            copies.add(copy);
        }

        return copies;
    }
}
