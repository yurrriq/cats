;; Copyright (c) 2014-2015 Andrey Antukh <niwi@niwi.nz>
;; Copyright (c) 2014-2015 Alejandro Gómez <alejandro@dialelo.com>
;; All rights reserved.
;;
;; Redistribution and use in source and binary forms, with or without
;; modification, are permitted provided that the following conditions
;; are met:
;;
;; 1. Redistributions of source code must retain the above copyright
;;    notice, this list of conditions and the following disclaimer.
;; 2. Redistributions in binary form must reproduce the above copyright
;;    notice, this list of conditions and the following disclaimer in the
;;    documentation and/or other materials provided with the distribution.
;;
;; THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
;; IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
;; OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
;; IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
;; INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
;; NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
;; DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
;; THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
;; (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
;; THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

(ns cats.data
  "Data structures that are used in various places of the library."
  (:require [cats.protocols :as p]
            [cats.context :as ctx]
            [cats.util :as util]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Pair type constructor and functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(declare context)

(deftype Pair [fst snd]
  #?(:clj  clojure.lang.Seqable
     :cljs cljs.core/ISeqable)
  (#?(:clj seq :cljs -seq) [_]
    (list fst snd))

  #?(:clj  clojure.lang.Indexed
     :cljs cljs.core/IIndexed)
  (#?(:clj nth :cljs -nth) [_ i]
    (case i
      0 fst
      1 snd
      (throw #?(:clj (IndexOutOfBoundsException.)
                :cljs (js/Error. "Out of index")))))

  (#?(:clj nth :cljs -nth) [_ i notfound]
    (case i
      0 fst
      1 snd
      notfound))

  #?(:clj  clojure.lang.Counted
     :cljs cljs.core/ICounted)
  (#?(:clj count :cljs -count) [_] 2)

  #?(:clj  java.lang.Object
     :cljs cljs.core/IEquiv)
  (#?(:clj equals :cljs -equiv) [this other]
    (if (instance? Pair other)
      (and (= (.-fst this) (.-fst other))
           (= (.-snd this) (.-snd other)))
      false))

  p/Printable
  (-repr [_]
    (str "#<Pair [" (pr-str fst) " " (pr-str snd) "]>"))

  p/Contextual
  (-get-context [data] context))

(alter-meta! #'->Pair assoc :private true)

(util/make-printable Pair)

(defn pair
  [fst snd]
  (Pair. fst snd))

(defn pair?
  [v]
  (instance? Pair v))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Context definitions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^{:no-doc true}
  context
  (reify
    p/Context
    (-get-level [_] ctx/+level-default+)

    p/Semigroup
    (-mappend [_ sv sv']
      (pair
        (p/-mappend (p/-get-context (.-fst sv)) (.-fst sv) (.-fst sv'))
        (p/-mappend (p/-get-context (.-snd sv)) (.-snd sv) (.-snd sv'))))

    p/Functor
    (-fmap [_ f mv]
      (pair (.-fst mv) (f (.-snd mv))))

    p/Foldable
    (-foldl [_ f z mv]
      (f z (.-snd mv)))

    (-foldr [_ f z mv]
      (f (.-snd mv) z))

    p/Traversable
    (-traverse [_ f mv]
      (let [a (f (.-snd mv))]
        (p/-fmap (p/-get-context a) #(pair (.-fst mv) %) a)))))

(defn pair-monoid
  "A pair monoid type constructor."
  [inner-monoid]
  (reify
    p/Context
    (-get-level [_]
      (+ (p/-get-level inner-monoid)
         ctx/+level-default+))

    p/Semigroup
    (-mappend [_ sv sv']
      (pair
       (p/-mappend inner-monoid (.-fst sv) (.-fst sv'))
       (p/-mappend inner-monoid (.-snd sv) (.-snd sv'))))

    p/Monoid
    (-mempty [_]
      (pair
       (p/-mempty inner-monoid)
       (p/-mempty inner-monoid)))))
