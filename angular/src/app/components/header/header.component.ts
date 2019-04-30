import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { LoginService } from '../../login/login.service';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent implements OnInit {

  page = "home";

  constructor(
    private loginService: LoginService,
    private router: Router,
  ) { }

  ngOnInit() {
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

  onTabClick(href: String) {
    this.router.navigate([href]);
    return false;
  }

  logout() {
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
