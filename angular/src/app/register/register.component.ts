import { Component, OnInit, ElementRef, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, NgForm, Validators, FormControl, ValidatorFn, ValidationErrors } from '@angular/forms';
import { Router } from '@angular/router';

import { UserService } from '../user/user.service';
import { Data } from 'src/assets/data/data';

@Component({
  selector: 'app-root',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})

export class RegisterComponent implements OnInit {
  update = false;
  submitButtonText = "Register";
  btnText = "Register";
  registerForm: FormGroup;
  messageType = "error";
  errorMessage = "";
  matchPasswordsCheck = true;
  socialUser = false;

  @ViewChild('divToScroll') divToScroll: ElementRef;

  constructor(
    private router: Router,
    private userService: UserService,
    private formBuilder: FormBuilder,
    private appData: Data,
  ) { }

  ngOnInit() {
    this.registerForm = this.formBuilder.group({
      "Name": ['', Validators.required],
      "Mail": ['', Validators.compose([
        Validators.required,
        Validators.email
      ])],
      "DisplayName": ['', Validators.required],
      "Password": ['', Validators.compose([
        Validators.required,
        Validators.minLength(8),
        Validators.maxLength(64)
      ])],
      "ConfirmPassword": ['', Validators.required],
      "MobileNumber": [''],
      "MFA": [false],
      "PasswordNeverExpire": [false],
      "ForcePasswordChangeNext": [false],
      "ServiceUser": [false],
      "SendEmailInvite": [false],
      "SendSmsInvite": [false],
      "Description": [''],
      "OfficeNumber": [''], // Validators.pattern('^(?=.*[0-9])[- +()0-9]+$')
      "HomeNumber": [''],
      "ReportsTo": [''],
      "InSysAdminRole": [false],
      "InEverybodyRole": [true]
    }, { updateOn: 'blur' });

    if (this.appData.login && this.appData.login.userId && this.appData.login.userId != "") {
      this.userService.getById(this.appData.login.userId, this.appData.login.social).subscribe(
        data => {
          if (data.success) {
            let userControls = this.registerForm.controls;
            let user = data.Result;
            if (user.DirectoryServiceType == "FDS") {
              this.registerForm.disable();
              this.socialUser = true;
            }
            this.socialUser = user.DirectoryServiceType == "FDS";
            userControls.Name.setValue(user.Name);
            if (this.appData.login.social) {
              userControls.Mail.setValue(user.EmailAddress);
            } else {
              userControls.Mail.setValue(user.Mail);
            }
            userControls.DisplayName.setValue(user.DisplayName);
            userControls.MobileNumber.setValue(user.MobileNumber);
            userControls.MFA.setValue(user.MFA);
          } else {
            this.setMessage("error", data.Message);
          }
        },
        error => {
          this.setMessage("error", error.message);
        }
      );
      this.update = true;
      this.submitButtonText = "Update";
    } else {
      this.update = false;
      this.submitButtonText = "Register";
      this.registerForm.reset();
    }
  }

  pick(obj: {}, keys) {
    return Object.assign({}, ...keys.map(k => k in obj ? { [k]: obj[k] } : {}))
  }

  checkMessageType() {
    return this.messageType == "info";
  }

  matchPasswords() {
    if (this.registerForm.controls.ConfirmPassword.pristine) {
      return;
    }
    let pass = this.registerForm.controls.Password.value;
    let confirmPass = this.registerForm.controls.ConfirmPassword.value;

    return this.matchPasswordsCheck = pass === confirmPass; // ? null : { notSame: true }
  }

  registerUser(form: NgForm) {
    if (!this.update) {
      this.validateAllFormFields(this.registerForm);
      this.matchPasswords();
      if (this.registerForm.invalid || !this.matchPasswordsCheck) {
        return;
      }
    }

    let user;
    if (this.update) {
      if (this.socialUser) {
        return;
      }
      let fieldArray = ["Name", "Mail", "DisplayName", "MobileNumber", "MFA"];
      if (!this.validateFormFields(fieldArray)) {
        this.divToScroll.nativeElement.scrollTop = 0;
        return;
      }
      user = this.pick(form, fieldArray)
      this.userService.update(user, this.appData.login.userId).subscribe(
        data => {
          if (data.success == true) {
            this.setMessage("info", "User updated successfully");
            this.router.navigate(['/user']);
          } else {
            this.setMessage("error", data.Message);
          }
        },
        error => {
          this.setMessage("error", error.message);
        }
      );
    } else {
      user = Object.assign({}, form);
      // let username = user.Name;
      this.userService.register(user).subscribe(
        data => {
          if (data.success == true) {
            this.appData.register = {
              "messageType": "info",
              "message": "User " + user.Name + " registered successfully. Enter your credentials here to proceed."
            };
            this.router.navigate(['/login']);
          } else {
            this.setMessage("error", data.Message);
          }
        },
        error => {
          this.setMessage("error", error.message);
        }
      );
    }
  }
  setMessage(messageType: string, message: string) {
    this.messageType = messageType;
    this.errorMessage = message;
    this.divToScroll.nativeElement.scrollTop = 0;
  }

  cancelRegister() {
    this.registerForm.reset();
    if (this.update) {
      this.router.navigate(['dashboard']);
    } else {
      this.router.navigate(['/']);
    }
  }

  // #TODO Move in common util
  validateFormFields(controls: Array<string>): boolean {
    let valid = true;
    for (let i = 0; i < controls.length; i++) {
      let field = this.registerForm.get(controls[i]);
      field.markAsTouched({ onlySelf: true });
      if (field.invalid) {
        valid = false;
      }
    }
    return valid;
  }

  // #TODO Move in common util
  validateAllFormFields(registerForm: FormGroup): any {
    Object.keys(registerForm.controls).forEach(field => {
      const control = registerForm.get(field);
      if (control instanceof FormControl) {
        control.markAsTouched({ onlySelf: true });
      } else if (control instanceof FormGroup) {
        this.validateAllFormFields(control);
      }
    });
  }

  // #TODO Move in common util
  public hasError = (controlName: string, errorName: string) => {
    let form = this.registerForm;
    let control = form.controls[controlName];
    return ((control.invalid && (control.dirty || control.touched)) && control.hasError(errorName));
  }
}
