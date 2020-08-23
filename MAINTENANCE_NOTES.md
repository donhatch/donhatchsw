Current (very fiddly) recipe for updating the java6..java11 branches (all the same)
===================================================================================

```
git checkout master

git status -uno  (should be no modified files)
make clean
git checkout java6
git merge master
# It will make a commit with "Merge branch 'master' into java1.6".  Commit it.
make clean
make  # .java files that disappeared wrt last time will get deleted during the following git commit -a
git add */*/*/*.java{,.lines}  # makes it so new .java files will get added
git commit -a --amend  # change to "add (currently same as java7...java11), and update precompiled .java files"

git push . java6:java7
git push . java6:java8
git push . java6:java9
git push . java6:java10
git push . java6:java11

git push origin java6 java7 java8 java9 java10 java11
```


TODO: when I make clean and then git checkout master, and make,
I get some weird error about shims_for_deprecated files not existing? weird.
It was some sequence like the following, but I can't reproduce it at the moment:
```
git checkout java6
make clean
make
git checkout master
make
```
