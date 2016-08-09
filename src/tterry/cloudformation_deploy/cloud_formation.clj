(ns tterry.cloudformation-deploy.cloud-formation
  (:require [amazonica.aws.cloudformation :as cf]
            [clojure.java.io :as io]
            [leiningen.core.main :as lein]
            [clojure.set :as set])
  (:import (com.amazonaws AmazonServiceException)
           (java.io File)))

(def create-completed-statuses #{"CREATE_COMPLETE" "CREATE_FAILED" "ROLLBACK_FAILED"})
(def update-completed-statuses #{"UPDATE_COMPLETE" "UPDATE_ROLLBACK_COMPLETE" "UPDATE_ROLLBACK_FAILED"})

(def update-allowed-statuses (set/union create-completed-statuses update-completed-statuses))

(def STATUS_CHECK_INTERVAL (* 10 1000))
(def STATUS_CHECK_ATTEMPTS 100)

(def stack-status-filters ["CREATE_IN_PROGRESS"
                           "CREATE_FAILED"
                           "CREATE_COMPLETE"
                           "ROLLBACK_IN_PROGRESS"
                           "ROLLBACK_FAILED"
                           "ROLLBACK_COMPLETE"
                           "DELETE_IN_PROGRESS"
                           "DELETE_FAILED"
                           "UPDATE_IN_PROGRESS"
                           "UPDATE_COMPLETE_CLEANUP_IN_PROGRESS"
                           "UPDATE_COMPLETE"
                           "UPDATE_ROLLBACK_IN_PROGRESS"
                           "UPDATE_ROLLBACK_FAILED"
                           "UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS"
                           "UPDATE_ROLLBACK_COMPLETE"])
(defn endpoint-for-region [region]
  {:endpoint (str "cloudformation." region ".amazonaws.com")})

(defn stack-in-status [stack statuses]
  (contains? statuses (:stack-status stack)))

(defn stack [region stack-name]
  (try
    (-> (cf/describe-stacks (endpoint-for-region region)
                      :stack-name stack-name)
      :stacks
      first)
    (catch AmazonServiceException ase
      (when (not= (format "Stack with id %s does not exist" stack-name) (.getErrorMessage ase))
        (throw ase)))))

(defn stack-outputs [s]
  (let [outputs (:outputs s)]
    (when outputs
      (->> (map
             (fn[output] {(:output-key output)
                          (:output-value output)})
             outputs)
           (into {})))))

(defn map->cf-params [params]
  (map
    (fn[key] {:parameterKey (if (keyword key) (name key) key)
              :parameterValue (get params key)})
    (keys params)))

(defn load-template [path]
  (let [template-contents (cond
                            (= String (type path))
                            (when-let [template-resource (io/resource path)]
                              (slurp (io/file template-resource)))
                            (= File (type path))
                            (slurp path))]
    (if template-contents
      template-contents
      (lein/abort "Unable to read cloudformation template" path "from classpath or file"))))

(defn start-update [region name template-path parameters]
  (lein/info "Updating stack" name "from template" template-path)
  (let [template-contents (load-template template-path)
        cf-params (map->cf-params parameters)
        stack-id (:stack-id (cf/update-stack (endpoint-for-region region)
                                             :template-body template-contents
                                             :stack-name name
                                             :parameters cf-params
                                             :capabilities ["CAPABILITY_IAM"]))]
    (lein/info "Stack updated started with id" stack-id)
    stack-id))

(defn wait-for-statuses
  ([region stack-name statuses count]
   (if (> count STATUS_CHECK_ATTEMPTS)
     (do
       (lein/info "Gave up waiting for stack after" STATUS_CHECK_ATTEMPTS "attempts")
       false)
     (when-let [s (stack region stack-name)]
       (let [status (:stack-status s)]
         (lein/info "Checking status of stack " attempt " (inc count) " status " status")
         (if-not (or (contains? statuses status))
           (do
             (Thread/sleep STATUS_CHECK_INTERVAL)
             (recur region stack-name statuses (inc count)))
           true)))))
  ([region stack-name statuses]
   (lein/info "Waiting for stack to be in status" statuses)
   (wait-for-statuses region stack-name statuses 0)))

(defn start-create [region name template-path parameters]
  (lein/info "Creating stack" name "from template" template-path)

  (let [template-contents (load-template template-path)
        cf-params (map->cf-params parameters)
        stack-id (:stack-id (cf/create-stack (endpoint-for-region region)
                                             :template-body template-contents
                                             :stack-name name
                                             :parameters cf-params
                                             :capabilities ["CAPABILITY_IAM"]))]
    (lein/info "Stack creation started with id" stack-id)
    stack-id))




