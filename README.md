# knotation-ace
A Knotation editor built on Ace.js


### Notes

`ace.js` almost added as dependency as per https://clojurescript.org/reference/dependencies
Unfortunately, the structure of the library means doing some non-trivial custom stuff to actually get it `goog.provide`d (see https://github.com/frankhale/Frehley/blob/master/resources/scripts/editor.js), so for the short term, I'm putting it together under the assumption that the appropriate library is loaded into `js`. (Though I will want to verify if the naive approach just ends up working)
