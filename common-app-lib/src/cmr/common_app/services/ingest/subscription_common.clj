(ns cmr.common-app.services.ingest.subscription-common
  "This contains the code for the scheduled subscription code to be shared
   between metadata-db and ingest."
  (:require
   [clojure.string :as string]))

(defn normalize-parameters-v1
  "Take a list of URL parameters, and sort them."
  [parameter-string]
  (-> (if (string/starts-with? parameter-string "?")
        (subs parameter-string 1)
        parameter-string)
      (string/split #"&")
      sort
      (as-> $ (string/join "&" $))))
