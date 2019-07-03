import { Component, OnInit, AfterContentChecked } from '@angular/core';
import { FormGroup, FormBuilder, Validators, FormControl, NgForm, AbstractControl } from '@angular/forms';
import { LoginService } from './login.service';
import { Router, ActivatedRoute } from '@angular/router';
import { Data } from 'src/assets/data/data';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})

export class LoginComponent implements OnInit, AfterContentChecked {
  loginForm: FormGroup;
  loginButtonText: string;
  loginPage: string;
  sessionId: string;
  tenantId: string;
  challenges: Array<Object>;
  mechanisms: JSON;
  currentMechanism: JSON;
  answerLabel: string;
  answerErrorText: string;
  disableUsername: string;
  pollChallenge: any;
  textAnswer = false;
  authMessage = "";
  messageType = "error";
  forgotPasswordCheck = false;
  matchPasswordsCheck = true;
  social = false;
  allowForgotPassword = false;
  loading = false;
  secondAuthLoad = false;

  constructor(
    private formBuilder: FormBuilder,
    private loginService: LoginService,
    private router: Router,
    private route: ActivatedRoute,
    private appData: Data,
  ) { }

  ngOnInit() {
    this.route.queryParamMap.subscribe(
      param => {
        if (param.has("ExtIdpAuthChallengeState") && param.has("username") && param.has("customerId")) {
          this.loginService.socialLoginResume(param.get("ExtIdpAuthChallengeState"), param.get("username"), param.get("customerId")).subscribe(
            data => {
              if (data.success == true) {
                this.social = true;
                this.redirectToDashboard(data.Result);
              } else {
                this.onLoginError(data.Message);
              }
            },
            error => {
              this.onLoginError(this.getErrorMessage(error));
            }
          )
        }
      }
    );

    if (this.appData.register && this.appData.register.messageType) {
      this.messageType = this.appData.register.messageType;
      this.authMessage = this.appData.register.message;
      this.appData.register.message = "";
    }

    this.loginForm = this.formBuilder.group({
      username: ['', Validators.required],
      authMethod: ['', Validators.required],
      answer: ['', Validators.required],
      confirmPassword: ['', Validators.required]
    });

    if (this.loginPage == null) {
      this.loginPage = "username";
      this.loginButtonText = "Next";
    }
  }

  ngAfterContentChecked() {
    if (this.loginPage == "secondChallenge" && !this.secondAuthLoad) {
      this.authMethodChange();
      this.secondAuthLoad = true;
    }
  }

  // Getter for easy access to form fields
  get formControls() { return this.loginForm.controls; }

  showLoginComponent(currentPage: String) {
    let page = this.loginPage;
    if (page == "firstChallenge" || page == "secondChallenge") {
      page = "challenge";
    }
    return currentPage == page;
  }

  showForgotPassword(currentPage: String) {
    return this.showLoginComponent(currentPage) && this.allowForgotPassword;
  }

  checkMessageType() {
    return this.messageType == "info";
  }

  authMethodChange() {
    let selectedAuthMethod = this.mechanisms[this.formControls.authMethod.value];

    if (selectedAuthMethod && selectedAuthMethod.AnswerType == 'Text') {
      this.textAnswer = true;
      this.answerErrorText = "Answer"
      if (selectedAuthMethod.Name == 'SQ') {
        this.answerLabel = selectedAuthMethod.PromptMechChosen;
      } else if (selectedAuthMethod.Name == 'UP') {
        this.answerLabel = selectedAuthMethod.PromptMechChosen;
        this.answerErrorText = "Password";
      }
    } else {
      this.textAnswer = false;
      this.answerErrorText = "Code";
    }
  }

  loginUser(form: NgForm) {
    // stop here if form is invalid
    if (!this.validateFormFields(this.getFormFieldsArray(this.loginPage)) || (this.loginPage == "reset" && !this.matchPasswords())) {
      return;
    }

    this.authMessage = "";

    if (this.loginPage == "username") {
      this.loading = true;
      this.loginService.beginAuth(this.formControls.username.value).subscribe(
        data => {
          if (data.success == true) {
            this.loading = false;
            if (data.Result.Summary == "LoginSuccess") {
              this.redirectToDashboard(data.Result);
            } else {
              this.loginForm.get('username').disable();
              if (data.Result && data.Result.ClientHints && data.Result.ClientHints.AllowForgotPassword) {
                this.allowForgotPassword = true;
              }
              this.runAuthSuccessFlow(data);
            }
          } else {
            this.loading = false;
            this.onLoginError(data.Message);
          }
        },
        error => {
          this.loading = false;
          this.onLoginError(this.getErrorMessage(error));
        });
    } else {
      if (this.loginPage == "password" || this.loginPage == "reset") {
        this.currentMechanism = this.mechanisms[0];
      } else {
        this.currentMechanism = this.mechanisms[this.formControls.authMethod.value];
      }

      this.loginService.advanceAuth(this.sessionId, this.tenantId, this.currentMechanism["MechanismId"], this.getAction(this.currentMechanism["AnswerType"]), this.formControls.answer.value).subscribe(
        data => {
          if (data.success == true) {
            if (data.Result.Summary == "LoginSuccess") {
              this.redirectToDashboard(data.Result);
            } else {
              this.runAuthSuccessFlow(data);
            }
          } else {
            this.onLoginError(data.Message);
          }
        },
        error => {
          this.onLoginError(this.getErrorMessage(error));
        });
    }
  }

  getFormFieldsArray(loginPage: string): string[] {
    let fieldsArray = [];
    switch (loginPage) {
      case "username":
        fieldsArray = ["username"];
        break;
      case "password":
      case "firstChallengeCode":
      case "secondChallengeCode":
        fieldsArray = ["answer"];
        break;
      case "firstChallenge":
      case "secondChallenge":
        fieldsArray = ["authMethod"];
        if (this.textAnswer) {
          fieldsArray.push("answer");
        }
        break;
      case "reset":
        fieldsArray = ["answer"];
        fieldsArray.push("confirmPassword");
        break;
      default:
        break;
    }
    return fieldsArray;
  }

  getAction(answerType: string) {
    switch (answerType) {
      case "Text":
        return "Answer";
      case "StartTextOob":
        return "StartOOB";
    }
  }

  runAuthSuccessFlow(data) {
    switch (this.loginPage) {
      case "username":
        this.sessionId = data.Result.SessionId;
        this.tenantId = data.Result.TenantId;
        this.challenges = data.Result.Challenges;
        let challengeCount = this.challenges.length;
        this.mechanisms = this.challenges[0]["Mechanisms"];
        let firstMechanismsCount = Object.keys(this.mechanisms).length;

        if (challengeCount > 0 && firstMechanismsCount > 0) {
          if (firstMechanismsCount == 1 && this.mechanisms[0].Name == "UP") {
            this.textAnswer = true;
            this.answerLabel = this.mechanisms[0].PromptMechChosen;
            this.answerErrorText = "Password";
            this.loginPage = "password";
          } else {
            this.loginPage = "firstChallenge";
          }
        }
        this.router.navigate(['login']);
        break;
      case "password":
      case "firstChallenge":
      case "firstChallengeCode":
      case "secondChallenge":
      case "secondChallengeCode":
      case "reset":
        if (data.Result.Summary == "OobPending") {
          this.textAnswer = true;
          this.answerLabel = this.currentMechanism["PromptMechChosen"];
          this.loginPage = "firstChallengeCode";
          this.pollChallenge = this.loginService.getPollingChallenge(this.sessionId, this.tenantId, this.currentMechanism["MechanismId"]).subscribe(
            data => {
              if (data.success == true) {
                if (data.Result.Summary == "OobPending") {
                } else if (data.Result.Summary == "NewPackage") {
                  this.pollChallenge.unsubscribe();
                  this.redirectToNextPage(data);
                } else if (data.Result.Summary == "LoginSuccess") {
                  this.setUserDetails(data.Result);
                  this.pollChallenge.unsubscribe();
                  this.router.navigate(['dashboard']);
                }
              } else {
                this.pollChallenge.unsubscribe();
              }
            },
            error => {
              this.onLoginError(this.getErrorMessage(error));
            });
          this.router.navigate(['login']);
        } else if (data.Result.Summary == "NewPackage") {
          this.redirectToNextPage(data);
        } else if (data.Result.Summary == "LoginSuccess") {
          this.redirectToDashboard(data.Result);
        } else if (data.Result.Summary == "NoncommitalSuccess") {
          this.startOver();
          this.messageType = "info";
          this.authMessage = data.Result.ClientMessage;
        } else {
          this.onLoginError(data.Message);
        }
        break;
      default:
        this.router.navigate(['login']);
        break;
    }
  }

  startOver() {
    this.loginForm.reset();
    this.textAnswer = false;
    this.loginPage = "username";
    this.loginButtonText = "Next";
    this.loginForm.get('username').enable();
    this.router.navigate(['login']);
    return false;
  }

  redirectToNextPage(data) {
    this.loginForm.controls["answer"].reset();
    this.mechanisms = data.Result.Challenges[0]["Mechanisms"];
    if (this.mechanisms[0].Name == "RESET") {
      if (this.loginPage == "reset" && data.Result.ClientMessage) {
        this.onLoginError(data.Result.ClientMessage);
      }
      this.loginPage = "reset";
      this.answerLabel = "New Password";
      this.answerErrorText = "New Password";
      this.textAnswer = true;
      this.loginForm.controls["confirmPassword"].reset();
    } else {
      this.loginPage = "secondChallenge";
      this.textAnswer = false;
    }
    this.resetFormFields(this.getFormFieldsArray(this.loginPage));
    this.router.navigate(['login']);
  }

  getErrorMessage(error): string {
    let errorMessage = error.message;
    if (error.error && error.error.Message) {
      errorMessage = error.error.Message;
    }
    return errorMessage;
  }

  onLoginError(message) {
    this.authMessage = message;
    this.messageType = "error";
    this.resetFormFields(this.getFormFieldsArray(this.loginPage));
    if (this.loginPage && this.loginPage != "username" && this.loginPage != "reset") {
      this.startOver();
    }
  }

  forgotPassword() {
    this.forgotPasswordCheck = true;
    this.authMessage = "";
    this.textAnswer = false;
    this.loginService.advanceAuth(this.sessionId, this.tenantId, "", "", "").subscribe(
      data => {
        if (data.success == true) {
          this.sessionId = data.Result.SessionId;
          this.tenantId = data.Result.TenantId;
          this.challenges = data.Result.Challenges;
          this.mechanisms = this.challenges[0]["Mechanisms"];
          this.loginPage = "firstChallenge";
          this.router.navigate(['login']);
        } else {
          this.onLoginError(data.Message);
        }
      },
      error => {
        this.onLoginError(this.getErrorMessage(error));
      });
    return false;
  }

  matchPasswords() { //group: FormGroup
    if (this.loginForm.controls.confirmPassword.pristine) {
      return;
    }
    let pass = this.loginForm.controls.answer.value;
    let confirmPass = this.loginForm.controls.confirmPassword.value;

    return this.matchPasswordsCheck = pass === confirmPass; // ? null : { notSame: true }
  }

  socialLogin(name: string) {
    this.loginService.socialLogin(name).subscribe(
      data => {
        if (data.success == true) {
          document.location.href = data.Result.IdpRedirectUrl;
        } else {
          this.onLoginError(data.Message);
        }
      },
      error => {
        this.onLoginError(this.getErrorMessage(error));
      }
    );
  }

  redirectToDashboard(result: any) {
    this.setUserDetails(result);
    this.router.navigate(['dashboard']);
  }

  setUserDetails(result: any) {
    this.appData.login = {
      "userId": result.UserId,
      "username": result.User,
      "tenant": result.PodFqdn,
      "social": this.social
    };
  }

  resetFormFields(controls: Array<string>): boolean {
    console.log("Reset Form Fields");
    let valid = true;
    for (let i = 0; i < controls.length; i++) {
      let field = this.loginForm.get(controls[i]);
      field.markAsUntouched({ onlySelf: true });
      if (field.invalid) {
        valid = false;
      }
    }
    return valid;
  }

  validateFormFields(controls: Array<string>): boolean {
    let valid = true;
    for (let i = 0; i < controls.length; i++) {
      let field = this.loginForm.get(controls[i]);
      field.markAsTouched({ onlySelf: true });
      if (field.invalid) {
        valid = false;
      }
    }
    return valid;
  }

  // #TODO Move in common util
  validateAllFormFields(form: FormGroup): any {
    Object.keys(form.controls).forEach(field => {
      const control = form.get(field);
      if (control instanceof FormControl) {
        control.markAsTouched({ onlySelf: true });
      } else if (control instanceof FormGroup) {
        this.validateAllFormFields(control);
      }
    });
  }

  // #TODO Move in common util
  public hasError = (controlName: string, errorName: string) => {
    let form = this.loginForm;
    let control = form.controls[controlName];
    return ((control.invalid && (control.dirty || control.touched)) && control.hasError(errorName));
  }
}
