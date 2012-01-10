;;   Copyright (c) Zachary Tellman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns lamina.test.channel
  (:use
    [clojure test]
    [lamina.core channel])
  (:require
    [lamina.core.pipeline :as p]
    [criterium.core :as c]))

;;;

(defn sink []
  (let [a (atom [])]
    [a
     #(do
        (swap! a conj %)
        true)]))

;;;

(deftest test-map*
  (let [[v callback] (sink)
        a (channel 0 1 2)
        b (map* inc a)]
    (receive-all b callback)
    (enqueue a 3)
    (enqueue b 4)
    (is (= (range 1 6) @v))))

(deftest test-filter*
  (let [[v callback] (sink)
        a (channel 0 1 2)
        b (->> a (map* inc) (filter* even?))]
    (receive-all b callback)
    (enqueue a 3 4)
    (is (= [2 4] @v))))

(deftest test-fork
  (let [a (channel 0 1 2)
        b (->> a fork (map* inc))
        c (->> a fork (filter* even?))
        d (->> b fork (filter* even?))]
    (is (= [0 1 2] (channel-seq a)))
    (is (= [1 2 3] (channel-seq b)))
    (is (= [0 2] (channel-seq c)))
    (is (= [2] (channel-seq d)))))

;;;

(defmacro bench [name & body]
  `(do
     (println "\n-----\n lamina.core.channel -" ~name "\n-----\n")
     (c/quick-bench
       (do ~@body)
       :reduce-with #(and %1 %2))))

(deftest ^:benchmark benchmark-channels
  (bench "create channel"
    (channel))
  (bench "create and map*"
    (->> (channel) (map* inc)))
  (bench "create and fork"
    (->> (channel) fork))
  (let [ch (channel)]
    (receive-all ch (fn [_]))
    (bench "simple-enqueue"
      (enqueue ch :msg)))
  (bench "create and enqueue"
    (let [ch (channel)]
      (enqueue ch :msg)))
  (bench "create, read, and enqueue"
    (let [ch (channel)]
      (read-channel ch)
      (enqueue ch :msg)))
  (bench "create, enqueue, and read"
    (let [ch (channel)]
      (enqueue ch :msg)
      (read-channel ch)))
  (bench "create, receive-all, and enqueue"
    (let [ch (channel)]
      (receive-all ch (fn [_]))
      (enqueue ch :msg)))
  (bench "create, enqueue, and receive-all"
    (let [ch (channel)]
      (enqueue ch :msg)
      (receive-all ch (fn [_])))))

(deftest ^:benchmark benchmark-queues
  (let [ch (channel)]
    (bench "enqueue 1e3"
      (dotimes [_ 1e3]
        (enqueue ch 1))
      (channel-seq ch))
    (bench "enqueue 1e6"
      (dotimes [_ 1e6]
        (enqueue ch 1))
      (channel-seq ch))))
