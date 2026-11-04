package com.kinetirx.mapper;

import com.kinetirx.dto.InventoryDtos;
import com.kinetirx.entity.Distributor;
import com.kinetirx.entity.Medicine;
import com.kinetirx.entity.MedicineGroup;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Entity <-> DTO mapping for inventory. Keeps the Go numeric semantics (float64 on the wire). */
public final class InventoryMapper {

    private InventoryMapper() {}

    public static InventoryDtos.Medicine toDto(Medicine m) {
        return new InventoryDtos.Medicine(
                m.getId(), m.getName(), m.getCompany(), m.getDist(), m.getDistributorId(), m.getHsn(),
                m.getBatch(), m.getPack(), m.getSalt(), m.getGeneric(), m.getGroupName(), m.getRack(),
                d(m.getStock()), d(m.getRate()), d(m.getOmrp()), d(m.getMrp()), m.getScheme(),
                d(m.getGst()), d(m.getDisc()), d(m.getTabsPerStrip()), m.getExpiry(),
                m.isLabTest(), m.isTrackStock(), m.getItemType(), m.getBarcode(),
                m.getCreatedAt(), m.getUpdatedAt());
    }

    /** Copies every mutable field from the request, as the Go full-replace UPDATE does. */
    public static void apply(Medicine m, InventoryDtos.MedicineInput in, boolean trackStock, String itemType) {
        m.setName(in.name());
        m.setCompany(nz(in.company()));
        m.setDist(nz(in.dist()));
        m.setDistributorId(in.distributor());
        m.setHsn(nz(in.hsn()));
        m.setBatch(nz(in.batch()));
        m.setPack(nz(in.pack()));
        m.setSalt(nz(in.salt()));
        m.setGeneric(in.generic());
        m.setGroupName(nz(in.group()));
        m.setRack(nz(in.rack()));
        m.setStock(bd(in.stock(), 3));
        m.setRate(bd(in.rate(), 2));
        m.setOmrp(bd(in.omrp(), 2));
        m.setMrp(bd(in.mrp(), 2));
        m.setScheme(nz(in.scheme()));
        m.setGst(bd(in.gst(), 2));
        m.setDisc(bd(in.disc(), 2));
        m.setTabsPerStrip(bd(in.tabsPerStrip(), 2));
        m.setExpiry(nz(in.expiry()));
        m.setLabTest(in.isLabTest());
        m.setTrackStock(trackStock);
        m.setItemType(itemType);
        m.setBarcode(in.barcode());
    }

    public static InventoryDtos.MedicineGroup toDto(MedicineGroup g) {
        return new InventoryDtos.MedicineGroup(g.getId(), g.getName(), g.getCreatedAt(), g.getUpdatedAt());
    }

    public static InventoryDtos.Distributor toDto(Distributor d) {
        return new InventoryDtos.Distributor(d.getId(), d.getName(), d.getGstin(), d.getPhone(), d.getAddr(),
                d.getDlNo(), d.getEmail(), d.getContactPerson(), d.getRegisteredDate(), d.getSource(),
                d.getCreatedAt(), d.getUpdatedAt());
    }

    public static void apply(Distributor d, InventoryDtos.DistributorInput in) {
        d.setName(in.name());
        d.setGstin(nz(in.gstin()));
        d.setPhone(nz(in.phone()));
        d.setAddr(nz(in.addr()));
        d.setDlNo(in.dlNo());
        d.setEmail(in.email());
        d.setContactPerson(in.contactPerson());
        d.setRegisteredDate(in.registeredDate());
        d.setSource(in.source());
    }

    static double d(BigDecimal v) {
        return v == null ? 0d : v.doubleValue();
    }

    static BigDecimal bd(double v, int scale) {
        return BigDecimal.valueOf(v).setScale(scale, RoundingMode.HALF_UP);
    }

    static String nz(String s) {
        return s == null ? "" : s;
    }
}
