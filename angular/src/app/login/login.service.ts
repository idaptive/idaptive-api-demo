import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { interval } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})

export class LoginService {
  private authUrl = <YOUR_ZUUL_SERVICE_URL:PORT/auth/>;

  constructor(private http: HttpClient) { }

  beginAuth(User: string) {
    let head = new HttpHeaders()
      .set('Content-Type', 'application/json')
      .set('Accept', 'application/json')
      .set('Access-Control-Allow-Methods', 'POST')
      .set('Access-Control-Allow-Origin', '*')
      ;

    return this.http.post<any>(this.authUrl + `beginAuth`, { User, Version: "1.0" }, { headers: head })
      .pipe(map(user => {
        return user;
      }));
  }

  advanceAuth(SessionId: string, TenantId: string, MechanismId: string, Action: string, Answer: string) {
    let head = new HttpHeaders().set('Content-Type', 'application/json');

    if (!MechanismId || MechanismId == "") {
      return this.http.post<any>(this.authUrl + `advanceAuth`, { SessionId, TenantId, Action: "ForgotPassword" }, { headers: head, withCredentials: true, })
        .pipe(map(user => {
          return user;
        }));
    } else if (!Answer || Answer == "") {
      return this.http.post<any>(this.authUrl + `advanceAuth`, { SessionId, TenantId, MechanismId, Action }, { headers: head, withCredentials: true, })
        .pipe(map(user => {
          return user;
        }));
    }
    return this.http.post<any>(this.authUrl + `advanceAuth`, { SessionId, TenantId, MechanismId, Action: "Answer", Answer }, { headers: head, withCredentials: true, }) /*observe: 'response' */
      .pipe(map(user => {
        return user;
      }));
  }

  getPollingChallenge(SessionId: string, TenantId: string, MechanismId: string) {
    let head = new HttpHeaders().set('Content-Type', 'application/json');
    return interval(5000).pipe(switchMap(() => this.http.post<any>(this.authUrl + `advanceAuth`, { SessionId, TenantId, MechanismId, Action: "Poll" }, { headers: head, withCredentials: true, })
      .pipe(map(user => {
        return user;
      }))));
  }

  socialLogin(IdpName: string) {
    return this.http.get<any>(this.authUrl + `socialLogin/` + IdpName)
      .pipe(map(responseObj => {
        return responseObj;
      }));
  }

  socialLoginResume(ExtIdpAuthChallengeState: string, username: string, customerId: string) {
    let head = new HttpHeaders().set('Content-Type', 'application/json');
    return this.http.get<any>(this.authUrl + `socialLogin`, { headers: head, withCredentials: true, params: new HttpParams().set("ExtIdpAuthChallengeState", ExtIdpAuthChallengeState).set("username", username).set("customerId", customerId) })
      .pipe(map(responseObj => {
        return responseObj;
      }));
  }

  logout() {
    let head = new HttpHeaders().set('Content-Type', 'application/json');
    return this.http.post<any>(this.authUrl + `out`, {}, { headers: head, withCredentials: true, })
      .pipe(map(user => {
        return user;
      }));
  }
}
