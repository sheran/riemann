(ns riemann.openduty
  "Forwards events to Openduty"
  (:require [clj-http.client :as client])
  (:require [cheshire.core :as json]))

(defn- post
  "POST to the OpenDuty events API."
  [request event-url]
  (client/post event-url
               {:body (json/generate-string request)
                :socket-timeout 5000
                :conn-timeout 5000
                :content-type :json
                :accept :json
                :throw-entire-message? true}))

(defn- format-event
  "Formats an event for OD. event-type is one of :trigger, :acknowledge,
  :resolve"
  [service-key event-type event]
  {:service_key service-key
   :event_type event-type
   :incident_key (str (:host event) " " (:service event))
   :description (str (:host event) " "
                     (:service event) " is "
                     (:state event) " ("
                     (:metric event) ")")
   :details event})

(defn openduty
  "Creates an openduty adapter. Takes your OD service key, and returns a map of
  functions which trigger, acknowledge, and resolve events. Event service will
  be used as the incident key. The OD description will be the service, state,
  and metric. The full event will be attached as the details.

  (let [pd (pagerduty \"my-service-key\" \"event url\" )]
    (changed-state
      (where (state \"ok\") (:resolve pd))
      (where (state \"critical\") (:trigger pd))))"
  [service-key event-url]
  {:event-url event-url
   :trigger     (fn [e] (post (format-event service-key :trigger e) event-url))
   :acknowledge (fn [e] (post (format-event service-key :acknowledge e) event-url))
   :resolve     (fn [e] (post (format-event service-key :resolve e) event-url))})
