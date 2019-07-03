import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { LoginService } from '../../login/login.service';
import { Data } from 'src/assets/data/data';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent implements OnInit {
  page = "home";
  username = "";
  signOutMenu = false;
  homeMenu = false;

  constructor(
    private loginService: LoginService,
    private router: Router,
    private appData: Data,
  ) { }

  ngOnInit() {
    if (this.appData.login && this.appData.login.username) {
      this.username = this.appData.login.username.split("@")[0];
    }

    switch (this.router.url) {
      case "/":
      case "/login":
      case "/register":
        this.page = "home";
        break;
      case "/dashboard":
      case "/user":
        this.page = "dashboard";
        break;
    }
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
          this.router.navigate(['']);
        }
      },
      error => {
        console.log(error);
        // this.alertService.error(error);
      });
  }

}
