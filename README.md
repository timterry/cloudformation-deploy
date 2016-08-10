# cloudformation-deploy

A Leiningen plugin to deploy an AWS cloudformation stack via stack updates.

## Usage

Put `[cloudformation-deploy "0.1.0-SNAPSHOT"]` into the `:plugins` vector of your project.clj.

Sample plugin configuration
```
(defproject foo/bar "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.7.0"]]
  :plugins [[tterry/cloudformation-deploy "0.1.0-SNAPSHOT"]]
  :cloudformation-deploy {:parameters {:ecs-cluster-id "ABC"
                                       :app-version ~#(str (:version %))}
                           :stack-name "ABC-ABC"
                           :stack-template-path "cloudformation-test.json"
                           :region "eu-west-1"})
```
Stack parameters can include functions that take the project as a single argument.
Execute the plugin with:

    $ lein cloudformation-deploy

More than one set of configuration can be specified and chosen via a `:env` argument, for example:
```
(defproject foo/bar "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.7.0"]]
  :plugins [[tterry/cloudformation-deploy "0.1.0-SNAPSHOT"]]
  :cloudformation-deploy {:foo {:parameters {:ecs-cluster-id "ABC"
                                             :app-version ~#(str (:version %))}
                                :stack-name "ABC-ABC"
                                :stack-template-path "file:///tmp/cloudformation-test.json"
                                :region "eu-west-1"}})
```

    $ lein cloudformation-deploy :env :foo

`:stack-template-path` can be either a string to reference a classpath resource or prefix with `file:///`
to specify a file URI outside of the project classpath.

Stack parameters can be provided on the command line and will be merged with those defined in the project.clj.
They will override any found.

    $ lein cloudformation :env :foo :ecs-cluster-id "bar"

## License

Copyright © 2016 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
