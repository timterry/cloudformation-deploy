(ns tterry.cloudformation-deploy.deploy-stack
  (:require [leiningen.core.main :as lein]
            [tterry.cloudformation-deploy.cloud-formation :as cf]))

(defn deploy [params stack-name stack-template-path region]
  ;find stack by name
  ;if exists update
  ;if not exists create
  ;wait for completion

  (lein/info "Starting deploy with params" params "stack-name" stack-name "region" region)
  (let [existing-stack (cf/stack region stack-name)
        expected-statuses (if existing-stack cf/update-completed-statuses
                                             cf/create-completed-statuses)]
    (if existing-stack
      (do
        (lein/info "Existing stack found")
        (if (cf/stack-in-status existing-stack cf/update-allowed-statuses)
          (cf/start-update region stack-name stack-template-path params)
          (lein/abort "Stack is in status" (:status existing-stack) "and cannot be updated")))
      (do
        (lein/info "Existing stack not found")
        (cf/start-create region stack-name stack-template-path params)))
    (if (cf/wait-for-statuses region stack-name expected-statuses)
      (let [stack (cf/stack region stack-name)]
        (if (cf/unsuccessful-status? stack)
          (lein/abort "Stack creation/update failed")
          (lein/info "Stack outputs" (cf/stack-outputs stack))))
      (lein/abort "Stack creation/update failed, timeout waiting for stack to be in correct status")))
  (lein/info "Finished deploy."))


(comment
  (deploy {:applicationVersion "0.0.1"
           :puppetManifestVersion "0.0.1"}
          "TEST-STACK1"
          "cloudformation-test2.json"
          "eu-west-1")
  )