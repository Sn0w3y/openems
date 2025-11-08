import { ChangeDetectionStrategy, Component, OnDestroy, OnInit } from "@angular/core";
import { FieldType } from "@ngx-formly/core";
import { Subject, takeUntil } from "rxjs";

@Component({
    selector: "help-popover-label-with-description-and-checkbox",
    templateUrl: "./help-popover-label-with-description-and-checkbox.html",
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FormlyFieldCheckboxWithLabelComponent extends FieldType implements OnInit, OnDestroy {
    protected value: any;
    private destroy$ = new Subject<void>();

    public ngOnInit() {
        // If the default value is not set in beginning.
        this.value = this.formControl.value ?? this.field.defaultValue;

        // Listen to form control status changes to reset steps if disabled
        this.formControl.statusChanges.pipe(takeUntil(this.destroy$)).subscribe(status => {
            if (status === "DISABLED" && this.value !== false) {
                this.value = false;
                this.formControl.setValue(this.value);
                this.formControl.markAsDirty();
            }
        });
    }

    public ngOnDestroy() {
        this.destroy$.next();
        this.destroy$.complete();
    }

    /**
     * Needs to be updated manually, because @Angular Formly-Form doesnt do it on its own
     */
    protected updateFormControl(event: CustomEvent) {
        this.value = event.detail.checked;
        this.formControl.setValue(this.value);
        this.formControl.markAsDirty();
    }
}
