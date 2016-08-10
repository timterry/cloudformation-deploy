(ns leiningen.cloudformation-deploy
  (:require [tterry.cloudformation-deploy.deploy-stack :as deploy]
            [leiningen.core.main :as lein]
            [clojure.pprint]))

(defn str->keyword [str]
  (if (and str
           (.startsWith ^String str ":")
           (> (.length ^String str) 1))
    (keyword (.substring ^String str 1))
    str))

(defn eval-settings [project settings]
  (->> (map (fn[e]
              {(key e)
               (let [v (val e)]
                 (if (fn? v)
                   (v project)
                   v))})
            settings)
       (apply merge)))

(defn cloudformation-deploy
  [project & args]
  (lein/info "args\n" (with-out-str (clojure.pprint/pprint args)))
  (let [deploy-args (->> (partition-all 2 args)
                         (map (fn[arg-pair]
                                {(-> arg-pair first str->keyword) (-> arg-pair second str->keyword)}))
                         (apply merge))
        env (get deploy-args :env)
        settings (get-in project [:cloudformation-deploy env] (:cloudformation-deploy project))
        commandline-params (dissoc deploy-args :env)
        stack-params (merge (eval-settings project (:parameters settings))
                            commandline-params)]
    (deploy/deploy stack-params
                   (:stack-name settings)
                   (or (:stack-template-path settings) "cloudformation.json")
                   (:region settings))))