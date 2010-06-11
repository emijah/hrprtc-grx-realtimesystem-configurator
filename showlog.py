#!/usr/bin/python
# -*- coding:utf-8 -*-

import os

def showLog(year, month, day, user):
	d = year+'-'+month
	if day != None:
		d += '-'+day
	com = 'hg log -d '+ d
	if user != None:
		com += ' -u '+ user
	logs = shell(com)
		
	logmsg = []
	print
	print d
	for i in range(len(logs)):
		if logs[i].strip() != '':
			logmsg.append(logs[i])
		elif len(logmsg) > 0:
			info = HgLog(logmsg)
			print info.date + ":" + info.revision + "    " + info.summary
			logmsg = []

def shell(com, isVerbose = False):
    if isVerbose:
        print "$ " + com
    sin, sout = os.popen2(com)
    ret = list()
    while True:
        o = sout.readline()
        if o == '':
            sout.close()
            break;
        if isVerbose:
            print o,
        ret.append(o.rstrip("\n"))
    return ret

def makeDirectory(path):
    if path != '' and not os.path.exists(path):
        os.makedirs(path)

def YesOrNo(msg):
    while True:
        ans = raw_input(msg).lower()
        if ans == 'y' or ans == 'yes':
            return True
        elif ans == 'n' or ans == 'no':
            return False


class HgLog:
    def __init__(self, logMsg):
        self.changeset  = ""
        self.revision = ""
        self.shortId = ""
        self.branch = "default"
        self.parents = list()
        self.user = ""
        self.date = ""
        self.summary = ""
        for i,l in enumerate(logMsg):
            if l.startswith("changeset:"):
                self.changeSet = l.lstrip("changeset:").strip()
                tmp = self.changeSet.split(":")
                self.revision = tmp[0]
                self.shorId   = tmp[1]
            elif l.startswith("チェンジセット:"):
                self.changeSet = l.lstrip("チェンジセット:").strip()
                tmp = self.changeSet.split(":")
                self.revision = tmp[0]
                self.shorId   = tmp[1]
            elif l.startswith("branch:"):
                self.branch = l.lstrip("branch:").strip()
            elif l.startswith("ブランチ:"):
                self.branch = l.lstrip("ブランチ:").strip()
            elif l.startswith("parent:"):
                self.parents.append(l.lstrip("parent:").strip())
            elif l.startswith("親:"):
                self.parents.append(l.lstrip("親:").strip())
            elif l.startswith("user:"):
                self.user = l.lstrip("user:").strip()
            elif l.startswith("ユーザ:"):
                self.user = l.lstrip("ユーザ:").strip()
            elif l.startswith("date:"):
                self.date = l.lstrip("date:").strip()
            elif l.startswith("日付:"):
                self.date = l.lstrip("日付:").strip()
            elif l.startswith("summary:"):
                for s in logMsg[i:]:
                    self.summary += s
                self.summary = self.summary.lstrip("summary:").strip()
                break
            elif l.startswith("要約:"):
                for s in logMsg[i:]:
                    self.summary += s
                self.summary = self.summary.lstrip("要約:").strip()
                break


import sys
showLog('2010', sys.argv[1], None, user='kawasumi')

