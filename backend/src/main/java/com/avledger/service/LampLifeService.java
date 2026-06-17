package com.avledger.service;

import com.avledger.entity.Device;
import com.avledger.enums.DeviceType;
import com.avledger.repository.UsageRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LampLifeService {

    private final UsageRecordRepository usageRecordRepository;

    public Map<String, Object> getLampLifeInfo(Device device) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (device.getDeviceType() != DeviceType.PROJECTOR) {
            result.put("available", false);
            return result;
        }

        result.put("available", true);
        result.put("lampInstallDate", device.getLampInstallDate() != null ? device.getLampInstallDate().toString() : null);
        result.put("lampReplaceHours", device.getLampReplaceHours());

        LocalDate startDate = device.getLampInstallDate() != null ? device.getLampInstallDate() : device.getPurchaseDate();
        Integer totalMinutes = usageRecordRepository.sumDurationByDeviceIdAndDateAfter(device.getId(), startDate);
        int usedMinutes = totalMinutes != null ? totalMinutes : 0;
        double usedHours = Math.round(usedMinutes / 60.0 * 10) / 10.0;

        result.put("usedHours", usedHours);
        result.put("usedMinutes", usedMinutes);

        if (device.getLampReplaceHours() != null && device.getLampReplaceHours() > 0) {
            int replaceHours = device.getLampReplaceHours();
            double remainingHours = Math.max(0, Math.round((replaceHours - usedHours) * 10) / 10.0);
            double usedPercent = Math.min(100, Math.round(usedHours / replaceHours * 1000) / 10.0);
            double remainingPercent = Math.max(0, Math.round((100 - usedPercent) * 10) / 10.0);

            result.put("remainingHours", remainingHours);
            result.put("usedPercent", usedPercent);
            result.put("remainingPercent", remainingPercent);

            String status;
            if (usedPercent >= 100) {
                status = "EXPIRED";
            } else if (usedPercent >= 90) {
                status = "WARNING";
            } else if (usedPercent >= 70) {
                status = "CAUTION";
            } else {
                status = "NORMAL";
            }
            result.put("status", status);
        } else {
            result.put("remainingHours", null);
            result.put("usedPercent", null);
            result.put("remainingPercent", null);
            result.put("status", "UNSET");
        }

        return result;
    }
}
