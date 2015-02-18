(ns foursquare-1self.handler
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [cemerick.friend :as friend]
            [clj-http.client :as client]
            [friend-oauth2.workflow :as oauth2]
            [friend-oauth2.util :refer [format-config-uri get-access-token-from-params]]
            [cheshire.core :as j]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])))
(declare render-status-page)
(declare render-checkins-page)
(declare get-foursquare-checkins)
(defn credential-fn
  [token]
  ;;lookup token in DB or whatever to fetch appropriate :roles
  {:identity token :roles #{::user}})

(def client-config
  {:client-id     "DOWYGA5X3PVX3WXXDL0S3MMCZSAQBMJZHWYJSHLGU4B5O1BH"
   :client-secret "DPSNOMYFT2WETDZBIQTHAUW352C0CWJ5S2POQH1UHK2RZVES"
   :callback      {:domain "http://localhost:3000" :path "/auth/callback"}})

(def uri-config
  {:authentication-uri {:url   "https://foursquare.com/oauth2/authenticate"
                        :query {:client_id     (:client-id client-config)
                                :response_type "code"
                                :redirect_uri  (format-config-uri client-config)
                                :scope         "email"}}
   :access-token-uri   {:url   "https://foursquare.com/oauth2/access_token"
                        :query {:client_id     (:client-id client-config)
                                :client_secret (:client-secret client-config)
                                :grant_type    "authorization_code"
                                :redirect_uri  (format-config-uri client-config)}}})
(defroutes ring-app
           (GET "/" request "<a href=\"/repos\">My Github Repositories</a><br><a href=\"/status\">Status</a>")
           (GET "/status" request
                (render-status-page request))
           (GET "/repos" request
                (friend/authorize #{::user} (render-checkins-page request)))
           (friend/logout (ANY "/logout" request (ring.util.response/redirect "/"))))
(def app
  (handler/site
    (friend/authenticate
      ring-app
      {:allow-anon? true
       :workflows   [(oauth2/workflow
                       {:client-config        client-config
                        :uri-config           uri-config
                        :access-token-parsefn get-access-token-from-params
                        :credential-fn        credential-fn})]})))

(defn render-status-page [request]
  (let [count (:count (:session request) 0)
        session (assoc (:session request) :count (inc count))]
    (-> (ring.util.response/response
          (str "<p>We've hit the session page " (:count session)
               " times.</p><p>The current session: " session "</p>"))
        (assoc :session session))))

(defn render-checkins-page
  "Shows a list of the current users github repositories by calling the github api
   with the OAuth2 access token that the friend authentication has retrieved."
  [request]
  (let [authentications (get-in request [:session :cemerick.friend/identity :authentications])
        access-token (:access_token (second (first authentications)))
        checkin-response (get-foursquare-checkins access-token)]
    (str (vec (map :name checkin-response)))))

(defn get-foursquare-checkins
  "Github API call for the current authenticated users repository list."
  [access-token]
  (let [url (str "https://api.foursquare.com/v2/users/self/checkins?oauth_token=" access-token)
        response (client/get url {:accept :json})
        repos (j/parse-string (:body response) true)]
    repos))