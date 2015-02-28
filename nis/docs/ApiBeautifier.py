#
# helper script to generate TOC, and add links in proper h2 elements
#

from datetime import datetime
import re
from collections import OrderedDict
from bs4 import BeautifulSoup

def uncapitalize(s):
	if (s.upper() == s):
		return s
	return s[:1].lower() + s[1:]

def prettyName(t):
	return '-'.join(map(uncapitalize, t.split()))

r = re.compile(r'^(\s*)', re.MULTILINE)
def soupTab(s, encoding=None, formatter="minimal"):
	return s.encode(encoding)
#	return r.sub(lambda mobj: '\t'*len(mobj.group(1)), s.prettify(encoding, formatter))

def printToc(toc, indent=0):
	r = "\t"*indent + "<ol>\n";
	indent += 1 
	for k,v in toc.iteritems():
		r += ("\t" * (indent+1)) + "<li>"
		if len(k) > 0:
			r += "<a href='#%s'>%s</a>" % (prettyName(k), k)
		if v != {}:
			r += "\n" + printToc(v, indent+2)
			r += ("\t" * (indent+1)) + "</li>\n"
		else:
			r += "</li>\n"
	indent -= 1 
	r += "\t"*indent + "</ol>\n"
	return r


def main():
	o = OrderedDict()
	toc = OrderedDict({'root':o})
	parents = [o]

	def addObj(name):
		o = OrderedDict()
		parents[-1][name] = o
		parents.append(o)
		
	def adjust(level):
		while len(parents) > level:
			parents.pop()
		#while len(parents) != level:
		#	addObj('')

	soup = BeautifulSoup(open('NisApi.src.html'))
	for header in soup.findAll(['h1', 'h2', 'h3']):
		c = header.contents[0]
		header['id'] = prettyName(c)
		level = int(header.name[1])
		adjust(level)
		addObj(c)
	
	for header in soup.findAll(['h4']):
		if len(header.contents):
			c = header.contents[0]
			header['id'] = prettyName(c)

	e=soup.find(id='toc')
	e.clear()
	newtoc = BeautifulSoup(printToc(toc['root']))
	for elem in newtoc.html.body.contents:
		e.append(elem)

	for d in soup.findAll('time'):
		curTime = datetime.utcnow()
		d['datetime']=curTime.strftime('%Y-%m-%d %H:%M:00Z')
		d.clear()
		d.append( curTime.strftime('%H:%M, %B %d, %Y') )
	
	allResp = [e for e in soup.findAll('resp')]
	for e in allResp:
		pre = soup.new_tag("pre")
		samp = soup.new_tag("samp")
		samp['class'] = 'JSON'
		pre.append(samp)
		data = e.contents
		e.replace_with(pre)
		if len(data):
			data[0].replace_with(data[0].lstrip())
			data[-1].replace_with(data[-1].rstrip())
		print data
		samp.contents = data
		
	open('index.html', 'w').write(soupTab(soup, 'utf-8'))

if __name__ == "__main__":
    main()
