package io.openems.edge.controller.api.mqtt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

public class SendChannelValuesWorkerTest {

	@Test
	public void testDisappearedComponentsDetection() {
		// Simulate lastAllValues with two components: ess0 and meter0
		final Table<String, String, JsonElement> lastAllValues = ImmutableTable.<String, String, JsonElement>builder() //
				.put("ess0", "Soc", new JsonPrimitive(50)) //
				.put("ess0", "ActivePower", new JsonPrimitive(1000)) //
				.put("meter0", "ActivePower", new JsonPrimitive(500)) //
				.put("meter0", "Voltage", new JsonPrimitive(230)) //
				.build();

		// Simulate current allValues with only ess0 (meter0 disappeared)
		final Table<String, String, JsonElement> allValues = ImmutableTable.<String, String, JsonElement>builder() //
				.put("ess0", "Soc", new JsonPrimitive(55)) //
				.put("ess0", "ActivePower", new JsonPrimitive(1100)) //
				.build();

		// Collect topics that would be published with JsonNull
		final List<String> nullPublishedTopics = new ArrayList<>();
		final Set<String> currentComponentIds = allValues.rowKeySet();

		for (Entry<String, Map<String, JsonElement>> row : lastAllValues.rowMap().entrySet()) {
			if (!currentComponentIds.contains(row.getKey())) {
				// Component disappeared
				for (Entry<String, JsonElement> column : row.getValue().entrySet()) {
					nullPublishedTopics.add(row.getKey() + "/" + column.getKey());
				}
			}
		}

		// Verify meter0 channels are marked for JsonNull publishing
		assertEquals(2, nullPublishedTopics.size());
		assertTrue(nullPublishedTopics.contains("meter0/ActivePower"));
		assertTrue(nullPublishedTopics.contains("meter0/Voltage"));

		// Verify ess0 is NOT in the list (still exists)
		assertTrue(nullPublishedTopics.stream().noneMatch(t -> t.startsWith("ess0/")));
	}

	@Test
	public void testNoDisappearedComponents() {
		// Both cycles have the same components
		final Table<String, String, JsonElement> lastAllValues = ImmutableTable.<String, String, JsonElement>builder() //
				.put("ess0", "Soc", new JsonPrimitive(50)) //
				.build();

		final Table<String, String, JsonElement> allValues = ImmutableTable.<String, String, JsonElement>builder() //
				.put("ess0", "Soc", new JsonPrimitive(55)) //
				.build();

		final List<String> nullPublishedTopics = new ArrayList<>();
		final Set<String> currentComponentIds = allValues.rowKeySet();

		for (Entry<String, Map<String, JsonElement>> row : lastAllValues.rowMap().entrySet()) {
			if (!currentComponentIds.contains(row.getKey())) {
				for (Entry<String, JsonElement> column : row.getValue().entrySet()) {
					nullPublishedTopics.add(row.getKey() + "/" + column.getKey());
				}
			}
		}

		// No components disappeared
		assertTrue(nullPublishedTopics.isEmpty());
	}

	@Test
	public void testAllComponentsDisappeared() {
		// Last cycle had components, current has none
		final Table<String, String, JsonElement> lastAllValues = ImmutableTable.<String, String, JsonElement>builder() //
				.put("ess0", "Soc", new JsonPrimitive(50)) //
				.put("meter0", "ActivePower", new JsonPrimitive(500)) //
				.build();

		final Table<String, String, JsonElement> allValues = ImmutableTable.<String, String, JsonElement>of();

		final List<String> nullPublishedTopics = new ArrayList<>();
		final Set<String> currentComponentIds = allValues.rowKeySet();

		for (Entry<String, Map<String, JsonElement>> row : lastAllValues.rowMap().entrySet()) {
			if (!currentComponentIds.contains(row.getKey())) {
				for (Entry<String, JsonElement> column : row.getValue().entrySet()) {
					nullPublishedTopics.add(row.getKey() + "/" + column.getKey());
				}
			}
		}

		// All components disappeared
		assertEquals(2, nullPublishedTopics.size());
		assertTrue(nullPublishedTopics.contains("ess0/Soc"));
		assertTrue(nullPublishedTopics.contains("meter0/ActivePower"));
	}

	@Test
	public void testEmptyLastValues() {
		// First cycle - no previous values
		final Table<String, String, JsonElement> lastAllValues = ImmutableTable.<String, String, JsonElement>of();

		final Table<String, String, JsonElement> allValues = ImmutableTable.<String, String, JsonElement>builder() //
				.put("ess0", "Soc", new JsonPrimitive(50)) //
				.build();

		final List<String> nullPublishedTopics = new ArrayList<>();
		final Set<String> currentComponentIds = allValues.rowKeySet();

		for (Entry<String, Map<String, JsonElement>> row : lastAllValues.rowMap().entrySet()) {
			if (!currentComponentIds.contains(row.getKey())) {
				for (Entry<String, JsonElement> column : row.getValue().entrySet()) {
					nullPublishedTopics.add(row.getKey() + "/" + column.getKey());
				}
			}
		}

		// No previous values, nothing to clean up
		assertTrue(nullPublishedTopics.isEmpty());
	}

}
