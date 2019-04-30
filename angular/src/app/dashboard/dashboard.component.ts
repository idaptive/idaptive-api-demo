import { Component, OnInit } from '@angular/core';
import { UserService } from '../user/user.service';
import { Router } from '@angular/router';
import { Data } from 'src/assets/data/data';

@Component({
    selector: 'app-dashboard',
    templateUrl: './dashboard.component.html',
    styleUrls: ['./dashboard.component.css']
})

export class DashboardComponent implements OnInit {
    appList = [];

    constructor(
        private userService: UserService,
        private router: Router,
        private appData: Data,
    ) { }

    ngOnInit() {
        this.userService.getAllApps(this.appData.login.username).subscribe(
            data => {
                this.createAppList(data);
                // console.log(data);
            }, error => {

            }
        );
    }

    createAppList(data) {
        let tenant = this.appData.login.tenant;
        for (let key in data) {
            let app = {
                name: key,
                // uprest/HandleAppClick
                // run?appkey=b09552d2-fdde-4ef5-bc13-2b85ceb99805&amp;customerID=AAI0510
                url: "https://" + tenant + "/run?appkey=" + data[key][0].AppKey + "&amp;customerID=AAI0510",
                icon: "https://" + tenant + data[key][0].Icon
            }
            this.appList.push(app);
        }
        // console.log(this.appList);
    }

    checkSelectedTab(href: string) {
        if (this.router.url == href) {
            return true;
        }
    }
}
