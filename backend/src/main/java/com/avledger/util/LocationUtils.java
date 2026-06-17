package com.avledger.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LocationUtils {

    public static final List<String> STANDARD_LOCATIONS = Arrays.asList(
        "客厅", "影音室", "卧室", "书房", "厨房",
        "餐厅", "卫生间", "阳台", "储物间", "玄关"
    );

    private static final Map<String, List<String>> LOCATION_ALIAS_MAP = new HashMap<>();

    static {
        LOCATION_ALIAS_MAP.put("客厅", Arrays.asList(
            "客厅", "客厅电视墙", "客厅吊顶", "客厅设备柜", "客厅侧墙",
            "客厅墙角", "客厅沙发旁", "主客厅", "大客厅"
        ));
        LOCATION_ALIAS_MAP.put("影音室", Arrays.asList(
            "影音室", "家庭影院", "放映室", "视听室", "影视厅"
        ));
        LOCATION_ALIAS_MAP.put("卧室", Arrays.asList(
            "卧室", "主卧", "次卧", "主卧室", "次卧室",
            "卧室床头柜", "卧室书桌", "卧室电视柜", "小卧室", "大卧室"
        ));
        LOCATION_ALIAS_MAP.put("书房", Arrays.asList(
            "书房", "书房间", "工作间", "办公间", "办公室"
        ));
        LOCATION_ALIAS_MAP.put("厨房", Arrays.asList(
            "厨房", "厨房间", "中厨", "西厨"
        ));
        LOCATION_ALIAS_MAP.put("餐厅", Arrays.asList(
            "餐厅", "饭厅", "餐区"
        ));
        LOCATION_ALIAS_MAP.put("卫生间", Arrays.asList(
            "卫生间", "洗手间", "厕所", "浴室", "主卫", "客卫"
        ));
        LOCATION_ALIAS_MAP.put("阳台", Arrays.asList(
            "阳台", "南阳台", "北阳台", "生活阳台"
        ));
        LOCATION_ALIAS_MAP.put("储物间", Arrays.asList(
            "储物间", "储藏室", "杂物间", "仓库"
        ));
        LOCATION_ALIAS_MAP.put("玄关", Arrays.asList(
            "玄关", "门厅", "入口", "入户花园"
        ));
    }

    public static String normalizeLocation(String location) {
        if (location == null || location.isBlank()) {
            return "";
        }
        String trimmed = location.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        for (Map.Entry<String, List<String>> entry : LOCATION_ALIAS_MAP.entrySet()) {
            String standard = entry.getKey();
            List<String> aliases = entry.getValue();
            for (String alias : aliases) {
                if (trimmed.contains(alias) || alias.contains(trimmed)) {
                    return standard;
                }
            }
        }

        return trimmed;
    }

    public static List<String> sortLocations(List<String> locations) {
        List<String> standardOrder = STANDARD_LOCATIONS.stream()
            .filter(locations::contains)
            .collect(Collectors.toList());
        List<String> otherLocations = locations.stream()
            .filter(loc -> !STANDARD_LOCATIONS.contains(loc))
            .sorted()
            .collect(Collectors.toList());
        standardOrder.addAll(otherLocations);
        return standardOrder;
    }

    public static <T> Map<String, List<T>> groupByLocation(List<T> items, LocationExtractor<T> extractor) {
        Map<String, List<T>> groups = new LinkedHashMap<>();
        List<T> unassigned = new java.util.ArrayList<>();

        for (T item : items) {
            String location = extractor.getLocation(item);
            if (location != null && !location.isBlank()) {
                String normalized = normalizeLocation(location.trim());
                groups.computeIfAbsent(normalized, k -> new java.util.ArrayList<>()).add(item);
            } else {
                unassigned.add(item);
            }
        }

        if (!unassigned.isEmpty()) {
            groups.put("未分配位置", unassigned);
        }

        Map<String, List<T>> sorted = new LinkedHashMap<>();
        List<String> knownLocations = STANDARD_LOCATIONS.stream()
            .filter(groups::containsKey)
            .collect(Collectors.toList());
        List<String> otherLocations = groups.keySet().stream()
            .filter(loc -> !STANDARD_LOCATIONS.contains(loc) && !"未分配位置".equals(loc))
            .sorted()
            .collect(Collectors.toList());

        knownLocations.forEach(loc -> sorted.put(loc, groups.get(loc)));
        otherLocations.forEach(loc -> sorted.put(loc, groups.get(loc)));
        if (groups.containsKey("未分配位置")) {
            sorted.put("未分配位置", groups.get("未分配位置"));
        }

        return sorted;
    }

    public static List<String> getNormalizedLocationList(List<String> locations) {
        List<String> normalized = locations.stream()
            .map(LocationUtils::normalizeLocation)
            .filter(loc -> loc != null && !loc.isEmpty())
            .distinct()
            .collect(Collectors.toList());
        return sortLocations(normalized);
    }

    @FunctionalInterface
    public interface LocationExtractor<T> {
        String getLocation(T item);
    }
}
