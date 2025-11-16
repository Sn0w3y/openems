# Energy Monitor UI/UX Refactoring

## Overview
This refactoring addresses issue [#3387](https://github.com/OpenEMS/openems/issues/3387) where the gauge diagram in the Energy Monitor widget did not match the textual values and arrows.

## Problem Statement
The gauge bars were displaying values based on **historical maximum capacity** (powerRatio), while users expected to see **real-time power distribution** matching the textual values and flow arrows. This caused confusion as the visual representation did not align with the actual power flow.

### Example of the Issue
- Gauge bars showed: 1/3 from grid, 2/3 from PV
- Textual values showed: 4/5 from grid, 1/5 from PV
- The textual values were correct; the gauge visualization was misleading

## Root Cause
The `powerRatio` field represents:
- **Current power / Historical maximum power** for each component
- Each component (grid, production, consumption, storage) was scaled independently to its own historical maximum
- This meant the bars did not show the actual power distribution at that moment

## Solution Implemented

### 1. Added `distributionRatio` Field
A new field was added to the `Summary` interface to represent real-time power distribution:

```typescript
// ui/src/app/shared/type/defaulttypes.ts
production: {
    powerRatio: number,        // Historical max-based ratio [-1,1] (kept for backward compatibility)
    distributionRatio: number, // Real-time power distribution ratio [0,1] (NEW)
    // ... other fields
}
```

This was added to all four sections:
- **Production**: `distributionRatio` [0,1] - proportion of total system power from production
- **Grid**: `distributionRatio` [-1,1] - negative for buy, positive for sell
- **Consumption**: `distributionRatio` [0,1] - proportion of total system power consumed
- **Storage**: `distributionRatio` [-1,1] - negative for discharge, positive for charge

### 2. Calculate Distribution Ratios
Added calculation logic in `currentdata.ts`:

```typescript
// ui/src/app/shared/components/edge/currentdata.ts
result.production.distributionRatio = Utils.orElse(
    Utils.divideSafely(result.production.activePower, result.system.totalPower),
    0,
);
```

The `distributionRatio` is calculated as:
- **Current power / Total system power**
- Where `totalPower` = max(total power entering system, total power leaving system)

### 3. Updated Section Components
Modified all four section components to use `distributionRatio` instead of `powerRatio` for gauge visualization:

**Files Modified:**
- `ui/src/app/edge/live/energymonitor/chart/section/production.component.ts`
- `ui/src/app/edge/live/energymonitor/chart/section/grid.component.ts`
- `ui/src/app/edge/live/energymonitor/chart/section/consumption.component.ts`
- `ui/src/app/edge/live/energymonitor/chart/section/storage.component.ts`

**Change:**
```typescript
// Before
super.updateSectionData(
    sum.production.activePower,
    sum.production.powerRatio,  // ❌ Historical max-based
    arrowIndicate
);

// After
super.updateSectionData(
    sum.production.activePower,
    sum.production.distributionRatio,  // ✅ Real-time distribution
    arrowIndicate
);
```

### 4. Added Tooltips
Added SVG title elements to provide user guidance:

**Files Modified:**
- `ui/src/app/edge/live/energymonitor/chart/section/production.component.html`
- `ui/src/app/edge/live/energymonitor/chart/section/grid.component.html`
- `ui/src/app/edge/live/energymonitor/chart/section/consumption.component.html`
- `ui/src/app/edge/live/energymonitor/chart/section/storage.component.html`

**Change:**
```html
<svg:path [attr.d]="valuePath" fill="var(--ion-color-production)" stroke="var(--ion-color-production)">
  <svg:title>{{ 'EDGE.INDEX.ENERGYMONITOR.GAUGE_TOOLTIP' | translate }}</svg:title>
</svg:path>
```

### 5. Added Translations
Added tooltip text in English and German:

**English** (`ui/src/assets/i18n/en.json`):
```json
"GAUGE_TOOLTIP": "Gauge shows real-time power distribution relative to total system power"
```

**German** (`ui/src/assets/i18n/de.json`):
```json
"GAUGE_TOOLTIP": "Anzeige zeigt Echtzeit-Leistungsverteilung relativ zur Gesamtsystemleistung"
```

## Benefits

### ✅ Improved User Experience
- Gauge bars now match textual values and flow arrows
- Visual representation is intuitive and accurate
- No need to explain the visualization - it's self-evident

### ✅ Backward Compatibility
- `powerRatio` field is retained for any code that might depend on it
- No breaking changes to existing APIs
- Historical capacity utilization data is still available if needed

### ✅ Consistency
- All visual elements (gauges, arrows, text) now show the same information
- Real-time power distribution is clear at a glance

### ✅ User Guidance
- Tooltips provide context for users who want to understand the visualization
- Available in multiple languages

## Technical Details

### Data Flow
1. **Backend** provides raw power values via WebSocket
2. **Frontend** (`currentdata.ts`) calculates:
   - `system.totalPower` = max(power in, power out)
   - `distributionRatio` = component power / total power
3. **Section components** receive `distributionRatio` and pass to `updateSectionData()`
4. **Abstract section** (`abstractsection.component.ts`) renders gauge arc based on ratio
5. **SVG** displays the gauge with tooltip

### Calculation Example
```
Production: 2000W
Grid Buy: 8000W
Consumption: 10000W
Total Power: max(2000+8000, 10000) = 10000W

Production distributionRatio: 2000/10000 = 0.2 (20%)
Grid distributionRatio: -8000/10000 = -0.8 (-80%, negative = buy)
Consumption distributionRatio: 10000/10000 = 1.0 (100%)
```

The gauge bars will now show:
- Production: 20% of the arc
- Grid: 80% of the arc
- Consumption: 100% of the arc

This matches the textual values and user expectations!

## Files Changed

### Type Definitions
- `ui/src/app/shared/type/defaulttypes.ts` - Added `distributionRatio` to Summary interface

### Data Calculation
- `ui/src/app/shared/components/edge/currentdata.ts` - Calculate distributionRatio for all sections

### Component Logic
- `ui/src/app/edge/live/energymonitor/chart/section/production.component.ts`
- `ui/src/app/edge/live/energymonitor/chart/section/grid.component.ts`
- `ui/src/app/edge/live/energymonitor/chart/section/consumption.component.ts`
- `ui/src/app/edge/live/energymonitor/chart/section/storage.component.ts`

### Templates
- `ui/src/app/edge/live/energymonitor/chart/section/production.component.html`
- `ui/src/app/edge/live/energymonitor/chart/section/grid.component.html`
- `ui/src/app/edge/live/energymonitor/chart/section/consumption.component.html`
- `ui/src/app/edge/live/energymonitor/chart/section/storage.component.html`

### Translations
- `ui/src/assets/i18n/en.json`
- `ui/src/assets/i18n/de.json`

## Testing Recommendations

### Manual Testing
1. **Start OpenEMS Edge** with PV inverters and grid meter
2. **Open UI** and navigate to Energy Monitor
3. **Verify gauge bars** match the textual power values
4. **Test different scenarios:**
   - High PV production, low grid consumption
   - Low PV production, high grid consumption
   - Battery charging/discharging
   - Grid sell vs. grid buy
5. **Hover over gauge bars** to see tooltip
6. **Check multiple languages** (EN, DE)

### Expected Behavior
- Gauge bar lengths should be proportional to the power values shown in text
- If production is 2kW and consumption is 10kW, production gauge should be ~20% of consumption gauge
- Arrows should point in the same direction as indicated by gauge fill
- Tooltip should appear on hover (desktop) or long-press (mobile)

## Future Enhancements

### Potential Improvements
1. **Add percentage labels** to gauge bars for clarity
2. **Animate transitions** when power distribution changes
3. **Color coding** based on power source (renewable vs. grid)
4. **Historical comparison** overlay showing typical distribution
5. **Accessibility improvements** for screen readers

### Alternative Approaches Considered
1. **Use backend PowerDistribution channels** - More accurate but requires additional WebSocket subscriptions
2. **Rename powerRatio** - Would be breaking change, rejected
3. **Add toggle** to switch between historical and real-time view - Adds complexity, rejected

## Related Issues
- [#3387](https://github.com/OpenEMS/openems/issues/3387) - UI: dashboard energy monitor: gauge diagram does not match textual values
- [#3392](https://github.com/OpenEMS/openems/pull/3392) - [UI] Add tooltip to Energymonitor (superseded by this refactoring)

## Credits
- Issue reported by: @sjjh
- Discussion participants: @da-Kai, @Sn0w3y, @sfeilmeier
- Implementation: Ona (AI Assistant)
