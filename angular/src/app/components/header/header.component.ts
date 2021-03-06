import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { LoginService } from '../../login/login.service';
import { UserService } from 'src/app/user/user.service';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent implements OnInit {
  page = "home";
  name = "";
  signOutMenu = false;
  homeMenu = false;
  loading = false;
  custom = localStorage.getItem("custom") == "true";
  imageSource: SafeResourceUrl;
  accentColor = "#ffffff";
  ribbonColor = "#ffffff";

  constructor(
    private loginService: LoginService,
    private userService: UserService,
    private sanitizer: DomSanitizer,
    private router: Router
  ) { }

  ngOnInit() {
    if (localStorage.getItem("logo") === null || localStorage.getItem("logo") === "") {
      this.loading = true;
      this.userService.getClientCustomData().subscribe(
        data => {
          this.loading = false;
          if (data.appImage) {
            let logo = this.getTrimmedImageData(data.appImage);
            localStorage.setItem("logo", logo);
            this.imageSource = this.sanitizer.bypassSecurityTrustResourceUrl(logo);
            localStorage.setItem("accent", data.accentColor);
            this.accentColor = data.accentColor;
            localStorage.setItem("ribbon", data.ribbonColor);
            this.ribbonColor = data.ribbonColor;
          } else {
            console.log("Incorrect data response");
          }
        }, error => {
          this.loading = false;
          console.log("Error response");
        }
      );
    } else {
      this.imageSource = this.sanitizer.bypassSecurityTrustResourceUrl(localStorage.getItem("logo")) || "../../../assets/images/logo.png";
      this.accentColor = localStorage.getItem("accent") || "#ffffff";
      this.ribbonColor = localStorage.getItem("ribbon") || "#ffffff";
    }

    if (localStorage.getItem("displayName") !== null && localStorage.getItem("displayName") !== "") {
      this.name = localStorage.getItem("displayName");
    } else if (localStorage.getItem("username") !== null) {
      this.name = localStorage.getItem("username");
    }

    switch (this.router.url) {
      case "/":
      case "/login":
      case "/register":
        this.page = "home";
        break;
      case "/dashboard":
      case "/user":
      case "/custom":
        this.page = "dashboard";
        break;
    }
  }

  getTrimmedImageData(appImage) {
    return appImage.substr(1, appImage.length - 2);
  }

  checkPage(page: String) {
    return this.page == page;
  }

  checkSelectedTab(href: String) {
    if (this.router.url == href) {
      return true;
    }
  }

  notRegister() {
    return !this.checkSelectedTab('/register');
  }

  onTabClick(href: String) {
    this.router.navigate([href]);
    return false;
  }

  toggleHomeMenu() {
    return this.homeMenu = !this.homeMenu;
  }

  toggleSignOutMenu() {
    return this.signOutMenu = !this.signOutMenu;
  }

  logout() {
    this.signOutMenu = false;
    this.loginService.logout().subscribe(
      data => {
        if (data.success == true) {
          localStorage.clear();
          this.router.navigate(['']);
        }
      },
      error => {
        console.log(error);
      });
  }
}