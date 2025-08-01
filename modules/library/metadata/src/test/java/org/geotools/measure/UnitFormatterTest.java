/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2021, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.measure;

import static java.util.Arrays.asList;
import static javax.measure.MetricPrefix.MICRO;
import static org.junit.Assert.assertEquals;
import static si.uom.SI.ASTRONOMICAL_UNIT;
import static systems.uom.common.USCustomary.ELECTRICAL_HORSEPOWER;
import static systems.uom.common.USCustomary.REVOLUTION_PER_MINUTE;
import static tech.units.indriya.unit.Units.CUBIC_METRE;
import static tech.units.indriya.unit.Units.MONTH;
import static tech.units.indriya.unit.Units.SQUARE_METRE;

import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.measure.Unit;
import javax.measure.format.UnitFormat;
import org.junit.Test;
import si.uom.SI;
import tech.units.indriya.format.SimpleUnitFormat;
import tech.units.indriya.unit.Units;

public class UnitFormatterTest {

    @Test
    public void unitsOnlyInOld() throws Exception {
        SimpleUnitFormat simpleUnitFormat = SimpleUnitFormat.getInstance();

        List<Map.Entry<Unit<?>, String>> unitToName = toSortedList1(getUnitToNameMap(simpleUnitFormat));
        List<Map.Entry<Unit<?>, String>> unitToSymbol = toSortedList1(formatter.unitToName);

        List<Map.Entry<Unit<?>, String>> unitsOnlyInOld = unitToName.stream()
                .filter(entry -> !unitToSymbol.contains(entry))
                .collect(Collectors.toList());
        List<Map.Entry<Unit<?>, String>> indriyaBug = asList(
                entry(ASTRONOMICAL_UNIT, "UA"),
                entry(ELECTRICAL_HORSEPOWER, "hp(E)"),
                entry(Units.MONTH, "mo"),
                entry(SI.REVOLUTION, "rev"),
                entry(REVOLUTION_PER_MINUTE, "rpm"),
                entry(Units.GRAM.prefix(MICRO), "µg"),
                entry(Units.LITRE.prefix(MICRO), "µl"),
                entry(Units.CELSIUS.prefix(MICRO), "µ℃"));
        List<Map.Entry<Unit<?>, String>> unitsOnlyInOldWithoutBugs = new ArrayList<>(unitsOnlyInOld);
        unitsOnlyInOldWithoutBugs.removeAll(indriyaBug);
        assertEquals(
                unitsOnlyInOldWithoutBugs.size() + " units only in old: " + unitsOnlyInOldWithoutBugs + "\n",
                indriyaBug,
                unitsOnlyInOld);
    }

    @Test
    public void unitsOnlyInNew() throws Exception {
        SimpleUnitFormat simpleUnitFormat = SimpleUnitFormat.getInstance();

        List<Map.Entry<Unit<?>, String>> unitToNameMap = toSortedList1(getUnitToNameMap(simpleUnitFormat));
        List<Map.Entry<Unit<?>, String>> unitToSymbol = toSortedList1(formatter.unitToName);

        List<Map.Entry<Unit<?>, String>> unitsOnlyInNew = unitToSymbol.stream()
                .filter(entry -> !unitToNameMap.contains(entry))
                .collect(Collectors.toList());
        // only one kind of µ is added for those special-cased units in indriya:
        List<Map.Entry<? extends Unit<?>, String>> indriyaBug = asList(
                entry(Units.GRAM.prefix(MICRO), "μg"),
                entry(Units.LITRE.prefix(MICRO), "μl"),
                entry(Units.CELSIUS.prefix(MICRO), "μ℃"));
        List<Map.Entry<Unit<?>, String>> unitsOnlyInNewWithoutBugs = new ArrayList<>(unitsOnlyInNew);
        unitsOnlyInNewWithoutBugs.removeAll(indriyaBug);
        assertEquals(
                unitsOnlyInNewWithoutBugs.size() + " units only in new: " + unitsOnlyInNewWithoutBugs + "\n",
                indriyaBug,
                unitsOnlyInNew);
    }

    @Test
    public void namesOnlyInOld() throws Exception {
        SimpleUnitFormat simpleUnitFormat = SimpleUnitFormat.getInstance();

        List<Map.Entry<String, Unit<?>>> nameToUnitMap = toSortedList2(getNameToUnitMap(simpleUnitFormat));
        List<Map.Entry<String, Unit<?>>> symbolToUnit = toSortedList2(formatter.nameToUnit);

        List<Map.Entry<String, Unit<?>>> unitsOnlyInOld = nameToUnitMap.stream()
                .filter(entry -> !symbolToUnit.contains(entry))
                .collect(Collectors.toList());
        List<Map.Entry<String, Unit<?>>> indriyaBug = asList(
                entry("hp(E)", ELECTRICAL_HORSEPOWER),
                entry("month", MONTH),
                entry("mo", MONTH),
                entry("mon", MONTH),
                entry("m2", SQUARE_METRE),
                entry("rpm", REVOLUTION_PER_MINUTE),
                entry("m3", CUBIC_METRE));

        assertEquals(
                unitsOnlyInOld.size() + " names only in old: " + unitsOnlyInOld + "\n", indriyaBug, unitsOnlyInOld);
    }

    @Test
    public void namesOnlyInNew() throws Exception {
        SimpleUnitFormat simpleUnitFormat = SimpleUnitFormat.getInstance();

        List<Map.Entry<String, Unit<?>>> nameToUnitMap = toSortedList2(getNameToUnitMap(simpleUnitFormat));
        List<Map.Entry<String, Unit<?>>> symbolToUnit = toSortedList2(formatter.nameToUnit);

        List<Map.Entry<String, Unit<?>>> unitsOnlyInNew = symbolToUnit.stream()
                .filter(entry -> !nameToUnitMap.contains(entry))
                .collect(Collectors.toList());
        // only one kind of µ is added for those special-cased units in indriya:
        List<Map.Entry<String, Unit<?>>> indriyaBug = asList(
                entry("μg", Units.GRAM.prefix(MICRO)),
                entry("μl", Units.LITRE.prefix(MICRO)),
                entry("μ°C", Units.CELSIUS.prefix(MICRO)),
                entry("μ℃", Units.CELSIUS.prefix(MICRO)),
                entry("μOhm", Units.OHM.prefix(MICRO)));
        List<Map.Entry<String, Unit<?>>> unitsOnlyInNewWithoutBug = new ArrayList<>(unitsOnlyInNew);
        unitsOnlyInNewWithoutBug.removeAll(indriyaBug);
        assertEquals(
                unitsOnlyInNewWithoutBug.size() + " names only in new: " + unitsOnlyInNewWithoutBug + "\n",
                indriyaBug,
                unitsOnlyInNew);
    }

    private static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    @SuppressWarnings("unchecked") // reflection in use, cannot be type safe
    private static Map<Unit<?>, String> getUnitToNameMap(UnitFormat instance) throws Exception {
        Field unitToNameField = instance.getClass().getDeclaredField("unitToName");
        unitToNameField.setAccessible(true);
        return (HashMap<Unit<?>, String>) unitToNameField.get(instance);
    }

    @SuppressWarnings("unchecked") // reflection in use, cannot be type safe
    private static Map<String, Unit<?>> getNameToUnitMap(UnitFormat instance) throws Exception {
        Field nameToUnitField = instance.getClass().getDeclaredField("nameToUnit");
        nameToUnitField.setAccessible(true);
        return (HashMap<String, Unit<?>>) nameToUnitField.get(instance);
    }

    private static List<Map.Entry<Unit<?>, String>> toSortedList1(Map<Unit<?>, String> map) {
        return map.entrySet().stream()
                .sorted(Comparator.nullsFirst(
                        Comparator.comparing(x -> x.getKey().toString())))
                .collect(Collectors.toList());
    }

    private static List<Map.Entry<String, Unit<?>>> toSortedList2(Map<String, Unit<?>> map) {
        return map.entrySet().stream()
                .sorted(Comparator.nullsFirst(
                        Comparator.comparing(x -> x.getValue().toString())))
                .collect(Collectors.toList());
    }

    private static final BaseUnitFormatter formatter = new BaseUnitFormatter(Stream.of(
                    UnitDefinitions.DIMENSIONLESS,
                    UnitDefinitions.SI_BASE,
                    UnitDefinitions.CONSTANTS,
                    UnitDefinitions.SI_DERIVED,
                    UnitDefinitions.NON_SI,
                    UnitDefinitions.US_CUSTOMARY)
            .flatMap(Collection::stream)
            .collect(Collectors.toList()));
}
