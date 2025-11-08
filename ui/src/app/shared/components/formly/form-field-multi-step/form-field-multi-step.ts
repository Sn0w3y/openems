import { Component, OnInit } from "@angular/core";
import { FieldType } from "@ngx-formly/core";

@Component({
    selector: "form-field-multi-step",
    templateUrl: "./form-field-multi-step.html",
    standalone: false,
})
export class FormlyFieldMultiStepComponent extends FieldType implements OnInit {

    protected allStepsComplete: boolean = false;

    protected get steps() {
        return this.props.steps || [];
    }

    public ngOnInit() {
        // Ensure the model has an array to track steps
        const stepArray = this.formControl.value;

        if (!Array.isArray(stepArray)) {
            this.formControl.setValue(Array(this.steps.length).fill(false));
        }
        this.updateAllStepsComplete();
    }

    protected onCheckboxChange(event: any, index: number) {
        const updatedValue = this.formControl.value;
        updatedValue[index] = event.detail.checked;
        this.formControl.setValue(updatedValue);
        this.updateAllStepsComplete();
    }


    /**
     * Returns the show/hide value based on the properties.
     *
     * @returns boolean value representing "show" or "hide".
     */
    protected showContent(index: number) {
        return (!this.field.props?.disabled && !this.formControl.value[index]);
    }

    /**
     * Updates the allStepsComplete property based on current step values.
     * Cached to avoid recalculation on every change detection cycle.
     */
    private updateAllStepsComplete(): void {
        this.allStepsComplete = this.formControl.value.lastIndexOf(false) === -1;
    }

}
