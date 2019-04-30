import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';

import { User } from '../user/user';
import { map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})

export class UserService {

  constructor(private http: HttpClient) { }
  private userUrl = <YOUR_USER_SERVICE_URL:PORT/>;

  getAll() {
    let head = new HttpHeaders().set('Content-Type', 'application/json');
    return this.http.get<User[]>(this.userUrl, { headers: head, withCredentials: true });
  }

  getById(id: string, social: boolean) {
    let head = new HttpHeaders().set('Content-Type', 'application/json');
    let url = this.userUrl + `${id}`;
    if (social) {
      url = this.userUrl + `userinfo/${id}`;
    }
    return this.http.get<any>(url, { headers: head, withCredentials: true });
  }

  register(user: User) {
    let head = new HttpHeaders().set('Content-Type', 'application/json');
    return this.http.post<any>(this.userUrl, user, { headers: head, withCredentials: true });
  }

  update(user: {}, id: string) {
    let head = new HttpHeaders().set('Content-Type', 'application/json');
    return this.http.put<any>(this.userUrl + `${id}`, user, { headers: head, withCredentials: true });
  }

  delete(id: string) {
    return this.http.delete<any>(this.userUrl + `${id}`);
  }

  getAllApps(username: string) {
    let head = new HttpHeaders().set('Content-Type', 'application/json');
    return this.http.get<any>(this.userUrl + `user/dashboard`, { headers: head, withCredentials: true, params: new HttpParams().set("force", "true").set("username", username) })
      .pipe(map(data => {
        return data;
      }));
  }

}
