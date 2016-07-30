# cloudformation-deploy

A Leiningen plugin to do many wonderful things.

## Usage

Put `[cloudformation-deploy "0.1.0-SNAPSHOT"]` into the `:plugins` vector of your project.clj.

Sample plugin configuration
```
(defproject foo/bar "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.7.0"]]
  :plugins [[tterry/cloudformation-deploy "0.1.0-SNAPSHOT"]]
  :cloudformation-deploy {:foo {:parameters {:ecs-cluster-id "ABC"}
                                :stack-name "ABC-ABC"
                                :stack-template-path "cloudformation-test.json"
                                :region "eu-west-1"}})
```

Example usage

    $ lein cloudformation-deploy :env foo

## License

Copyright © 2016 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
