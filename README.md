Link Reverse!
=============

Mining of the CommonCrawl Corpus
---------------------------------

The mining of the common crawl corpus has been done in
[Spark](http://spark-project.org). [My experimental source code](https://github.com/namin/spark/tree/namin/namin/src/main/scala/net/namin/commoncrawl)
is available. I did various experiments with mining links in
documents, but at the end, settled on something relatively simple:
just show which pages link to a certain URL.

Results
-------

This [webapp](http://linkrev.herokuapp.com) shows the results. There
are two limitations: first, for tractability of the prototype, I am
only including links to the domain mit.edu. Second, I've only mined
the two first valid segments in CommonCrawl.
