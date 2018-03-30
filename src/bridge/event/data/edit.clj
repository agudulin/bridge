 (ns bridge.event.data.edit
  (:require [bridge.data.datomic :as datomic]
            [bridge.data.edit :as data.edit]
            [clj-time.core :as t]))

(require 'bridge.event.spec)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Edit

(def event-for-editing-pull-spec
  [:event/title
   :event/slug
   :event/status
   {:event/chapter [:chapter/slug]}
   {:event/organisers [:person/name]}
   :event/start-date
   :event/end-date
   :event/registration-close-date
   :event/details-markdown
   :event/notes-markdown])

(defn event-for-editing [db event-id]
  (datomic/pull db event-for-editing-pull-spec event-id))

(def edit-whitelist
  #{:event/title
    :event/slug
    :event/status
    :event/organisers
    :event/start-date
    :event/end-date
    :event/registration-close-date
    :event/details-markdown
    :event/notes-markdown})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Field validations

;;; :event/status

(def status->valid-next-status
  {:status/draft       #{:status/published   :status/cancelled}
   :status/published   #{:status/in-progress :status/cancelled}
   :status/in-progress #{:status/complete    :status/cancelled}})

(defmethod data.edit/check-custom-validation :event/status
  [db {:field/keys [entity-id value]}]
  (let [current-status       (datomic/attr db entity-id :event/status)
        possible-next-status (get status->valid-next-status current-status)]
    (cond (nil? possible-next-status)
          {:error :bridge.event.error/status-may-not-change}

          (not (contains? possible-next-status value))
          {:error                :bridge.event.error/invalid-next-status
           :possible-next-status possible-next-status
           :event/status         value})))

;;; :event/organisers

(defmethod data.edit/check-custom-validation :event/organisers
  [db {:field/keys [entity-id value retract?]}]
  (cond (and retract?
             (= 1 (count (datomic/attr db entity-id :event/organisers))))
        {:error            :bridge.event.error/event-can-not-have-no-organisers
         :field/retract?   true
         :event/organisers value}))

(comment

  (data.edit/check-field-update db edit-whitelist
                                #:field{:attr      .
                                        :value     .
                                        :entity-id .})

  :event/organisers               ; add/remove
  :event/details-markdown         ; edit, blank = auto retract
  :event/notes-markdown           ; edit, blank = auto retract

  (require '[bridge.dev.repl :as repl])

  (repl/set-datomic-mode! :peer)

  (def db (repl/db))
  (def attr :chapter/slug)
  (def value "clojurebridge-hermanus")
  )