(ns cmr.metadata-db.data.oracle.concepts.collection
  "Implements multi-method variations for collections"
  (:require [cmr.metadata-db.data.oracle.concepts :as c]
            [cmr.metadata-db.data.oracle.concept-tables :as tables]
            [cmr.common.log :refer (debug info warn error)]
            [cmr.common.date-time-parser :as p]
            [clj-time.coerce :as cr]
            [cmr.oracle.connection :as oracle]
            [cmr.metadata-db.data.concepts :as concepts]))

(defmethod c/db-result->concept-map :collection
  [concept-type db provider-id result]
  (some-> (c/db-result->concept-map :default db provider-id result)
          (assoc :concept-type :collection)
          (assoc-in [:extra-fields :short-name] (:short_name result))
          (assoc-in [:extra-fields :version-id] (:version_id result))
          (assoc-in [:extra-fields :entry-id] (:entry_id result))
          (assoc-in [:extra-fields :entry-title] (:entry_title result))
          (assoc-in [:extra-fields :delete-time]
                    (when (:delete_time result)
                      (c/oracle-timestamp->str-time db (:delete_time result))))))

(defmethod c/concept->insert-args [:collection false]
  [concept _]
  (let [{{:keys [short-name version-id entry-id entry-title delete-time]} :extra-fields} concept
        [cols values] (c/concept->common-insert-args concept)
        delete-time (when delete-time (cr/to-sql-time (p/parse-datetime delete-time)))]
    [(concat cols ["short_name" "version_id" "entry_id" "entry_title" "delete_time"])
     (concat values [short-name version-id entry-id entry-title delete-time])]))

(defmethod c/concept->insert-args [:collection true]
  [concept _]
  (let [{:keys [provider-id]} concept
        [cols values] (c/concept->insert-args concept false)]
    [(concat cols ["provider_id"])
     (concat values [provider-id])]))

(defmethod c/after-save :collection
  [db provider coll]
  (when (:deleted coll)
    ;; Cascade deletion to real deletes of granules
    (concepts/force-delete-by-params db
                                     provider
                                     {:concept-type :granule
                                      :provider-id (:provider-id coll)
                                      :parent-collection-id (:concept-id coll)})))
