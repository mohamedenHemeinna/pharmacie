package com.kinetirx.service;

import com.kinetirx.common.EventBus;
import com.kinetirx.dto.InventoryDtos;
import com.kinetirx.entity.Distributor;
import com.kinetirx.entity.Medicine;
import com.kinetirx.entity.MedicineGroup;
import com.kinetirx.exception.ApiException;
import com.kinetirx.mapper.InventoryMapper;
import com.kinetirx.repository.DistributorRepository;
import com.kinetirx.repository.MedicineGroupRepository;
import com.kinetirx.repository.MedicineRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Medicines, medicine groups, and distributors. Mirrors handlers/medicines.go, medicine_groups.go, distributors.go. */
@Service
public class InventoryService {

    private final MedicineRepository medicines;
    private final MedicineGroupRepository groups;
    private final DistributorRepository distributors;
    private final EventBus events;

    public InventoryService(MedicineRepository medicines, MedicineGroupRepository groups,
                            DistributorRepository distributors, EventBus events) {
        this.medicines = medicines;
        this.groups = groups;
        this.distributors = distributors;
        this.events = events;
    }

    // ---- medicines -------------------------------------------------------

    @Transactional(readOnly = true)
    public List<InventoryDtos.Medicine> listMedicines() {
        return medicines.findAllByOrderByNameAsc().stream().map(InventoryMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public InventoryDtos.Medicine getMedicine(String id) {
        return medicines.findById(id).map(InventoryMapper::toDto)
                .orElseThrow(() -> ApiException.notFound("Medicine not found"));
    }

    @Transactional
    public InventoryDtos.Medicine createMedicine(InventoryDtos.MedicineInput in) {
        requireName(in.name(), "Invalid medicine payload: name is required");
        Medicine m = new Medicine();
        m.setId(blankToNewId(in.id()));
        InventoryMapper.apply(m, in, trackStockOf(in), itemTypeOf(in));
        try {
            Medicine saved = medicines.saveAndFlush(m);
            events.publish("medicines");
            return InventoryMapper.toDto(saved);
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict(
                    "Failed to create medicine (id may already exist, or barcode already assigned to another item)");
        }
    }

    @Transactional
    public InventoryDtos.Medicine updateMedicine(String id, InventoryDtos.MedicineInput in) {
        requireName(in.name(), "Invalid medicine payload: name is required");
        Medicine m = medicines.findById(id).orElseThrow(() -> ApiException.notFound("Medicine not found"));
        InventoryMapper.apply(m, in, trackStockOf(in), itemTypeOf(in));
        try {
            Medicine saved = medicines.saveAndFlush(m);
            events.publish("medicines");
            return InventoryMapper.toDto(saved);
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.internal("Failed to update medicine (a duplicate barcode is a likely cause)");
        }
    }

    @Transactional
    public void deleteMedicine(String id) {
        if (!medicines.existsById(id)) throw ApiException.notFound("Medicine not found");
        medicines.deleteById(id);
        events.publish("medicines");
    }

    // ---- medicine groups -------------------------------------------------

    @Transactional(readOnly = true)
    public List<InventoryDtos.MedicineGroup> listGroups() {
        return groups.findAllByOrderByNameAsc().stream().map(InventoryMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public InventoryDtos.MedicineGroup getGroup(String id) {
        return groups.findById(id).map(InventoryMapper::toDto)
                .orElseThrow(() -> ApiException.notFound("Medicine group not found"));
    }

    @Transactional
    public InventoryDtos.MedicineGroup createGroup(InventoryDtos.MedicineGroupInput in) {
        requireName(in.name(), "Invalid medicine group payload: name is required");
        MedicineGroup g = new MedicineGroup();
        g.setId(blankToNewId(in.id()));
        g.setName(in.name());
        try {
            return InventoryMapper.toDto(groups.saveAndFlush(g));
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("Failed to create medicine group (name may already exist)");
        }
    }

    @Transactional
    public InventoryDtos.MedicineGroup updateGroup(String id, InventoryDtos.MedicineGroupInput in) {
        requireName(in.name(), "Invalid medicine group payload: name is required");
        MedicineGroup g = groups.findById(id).orElseThrow(() -> ApiException.notFound("Medicine group not found"));
        g.setName(in.name());
        try {
            return InventoryMapper.toDto(groups.saveAndFlush(g));
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.internal("Failed to update medicine group (name may already exist)");
        }
    }

    @Transactional
    public void deleteGroup(String id) {
        if (!groups.existsById(id)) throw ApiException.notFound("Medicine group not found");
        groups.deleteById(id);
    }

    // ---- distributors ----------------------------------------------------

    @Transactional(readOnly = true)
    public List<InventoryDtos.Distributor> listDistributors() {
        return distributors.findAllByOrderByNameAsc().stream().map(InventoryMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public InventoryDtos.Distributor getDistributor(String id) {
        return distributors.findById(id).map(InventoryMapper::toDto)
                .orElseThrow(() -> ApiException.notFound("Distributor not found"));
    }

    @Transactional
    public InventoryDtos.Distributor createDistributor(InventoryDtos.DistributorInput in) {
        requireName(in.name(), "Invalid distributor payload: name is required");
        Distributor d = new Distributor();
        d.setId(blankToNewId(in.id()));
        InventoryMapper.apply(d, in);
        try {
            return InventoryMapper.toDto(distributors.saveAndFlush(d));
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("Failed to create distributor (id may already exist)");
        }
    }

    @Transactional
    public InventoryDtos.Distributor updateDistributor(String id, InventoryDtos.DistributorInput in) {
        requireName(in.name(), "Invalid distributor payload: name is required");
        Distributor d = distributors.findById(id).orElseThrow(() -> ApiException.notFound("Distributor not found"));
        InventoryMapper.apply(d, in);
        try {
            return InventoryMapper.toDto(distributors.saveAndFlush(d));
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.internal("Failed to update distributor");
        }
    }

    @Transactional
    public void deleteDistributor(String id) {
        if (!distributors.existsById(id)) throw ApiException.notFound("Distributor not found");
        distributors.deleteById(id);
    }

    // ---- helpers ---------------------------------------------------------

    private static void requireName(String name, String title) {
        if (name == null || name.isBlank()) throw ApiException.badRequest(title);
    }

    static String blankToNewId(String id) {
        return id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
    }

    private static boolean trackStockOf(InventoryDtos.MedicineInput in) {
        return in.trackStock() == null || in.trackStock();
    }

    private static String itemTypeOf(InventoryDtos.MedicineInput in) {
        return in.itemType() == null || in.itemType().isBlank() ? "medicine" : in.itemType();
    }
}
