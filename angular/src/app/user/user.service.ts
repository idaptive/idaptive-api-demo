import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';

import { User } from '../user/user';
import { map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})

export class UserService {

  constructor(private http: HttpClient) { }
  private userUrl = "https://apidemo.idaptive.app:8762/user/";
  private userOpsUrl = "https://apidemo.idaptive.app:8762/userops/";

  getById(id: string, social: boolean) {
    let head = new HttpHeaders().set('Content-Type', 'application/json');
    let url = this.userOpsUrl + `${id}`;
    if (social) {
      url = this.userOpsUrl + `info/${id}`;
    }
    return this.http.get<any>(url, { headers: head, withCredentials: true });
  }

  register(user: User) {
    let head = new HttpHeaders().set('Content-Type', 'application/json');
    return this.http.post<any>(this.userUrl + `register`, user, { headers: head, withCredentials: true });
  }

  update(user: {}, id: string) {
    let head = new HttpHeaders().set('Content-Type', 'application/json');
    return this.http.put<any>(this.userOpsUrl + `${id}`, user, { headers: head, withCredentials: true });
  }

  getAllApps(username: string) {
    let head = new HttpHeaders().set('Content-Type', 'application/json');
    return this.http.get<any>(this.userOpsUrl + `dashboard`, { headers: head, withCredentials: true, params: new HttpParams().set("force", "true").set("username", username) })
      .pipe(map(data => {
        return data;
      }));
  }

  getClientCustomData() {
    let head = new HttpHeaders().set('Content-Type', 'application/json');
    let url = this.userUrl + `getclientconfig`;
    return this.http.get<any>(url, { headers: head, withCredentials: true });
  }

  getCustomData() {
    let head = new HttpHeaders().set('Content-Type', 'application/json');
    let url = this.userOpsUrl + `getconfig`;
    return this.http.get<any>(url, { headers: head, withCredentials: true });
  }

  setCustomData(custom: any) {
    let head = new HttpHeaders().set('Content-Type', 'application/json');
    return this.http.put<any>(this.userOpsUrl + `updateconfig`, custom, { headers: head, withCredentials: true });
  }

  refreshActuators() {
    this.http.post<any>(this.userUrl + `refresh`, { withCredentials: true }).subscribe();
    this.http.post<any>(this.userUrl + `actuator/refresh`, { withCredentials: true }).subscribe();
  }
}
