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

def convertDesc(s):
	descGroups = [f for f in s.findAll('desc')]
	for descGroup in descGroups:
		temp = [f for f in descGroup.contents]

		h = s.new_tag('h4')
		h.append('Description:')
		descGroup.replace_with(h)

		prev = h
		for elem in temp:
			prev.insert_after(elem)
			prev = elem

def convertVals(s, f):
	valGroups = [i for i in f.findAll('vals')]
	for valGroup in valGroups:
		div = s.new_tag('div')
		continue
		for elem in filter(lambda s: len(s)>0, map(lambda s: s.strip(), valGroup.string.split('\n'))):
			data = elem.split(':', 1)
			sep = s.new_tag('span')
			sep['class'] = 'sep'
			sep.append(' ')

			p = s.new_tag('p')
			p['class'] = 'NemNoSpacing FakeList'
			p.append(data[0] + ":")
			p.append(sep)
			p.append(data[1])
			div.append(p)
		valGroup.replace_with(div)

def convertFields(s):
	fieldGroups = [f for f in s.findAll('fields')]
	for fieldGroup in fieldGroups:
		t = s.new_tag('table')
		t['class'] = 'NemParamsGrid'
		fs = [temp for temp in fieldGroup.findAll('f')]
		fieldGroup.replaceWith(t)

		for f in fs:
			tr = s.new_tag('tr')
			param = s.new_tag('td')
			desc = s.new_tag('td')

			param.append(f['name'])
			convertVals(s, f)
			data = [temp for temp in f.contents]
			for elem in data:
				desc.append(elem)

			tr.append(param)
			tr.append(desc)
			t.append(tr)
		h = s.new_tag('h4')
		h.append('Description of the fields:')
		t.insert_before(h)
	
def convertJson(s):
	allResp = [e for e in s.findAll('resp')]
	print len(allResp)
	for e in allResp:
		pre = s.new_tag("pre")
		samp = s.new_tag("samp")
		samp['class'] = 'JSON'
		pre.append(samp)
		data = e.contents
		e.replace_with(pre)
		if len(data):
			data[0].replace_with(data[0].lstrip())
			data[-1].replace_with(data[-1].rstrip())
		samp.contents = data
	
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
	
	#for header in soup.findAll(['h4']):
	#	if len(header.contents):
	#		c = header.contents[0]
	#		header['id'] = prettyName(c)

	e=soup.find(id='toc')
	e.clear()
	newtoc = BeautifulSoup(printToc(toc['root']))
	if 'html' in newtoc:
		e.contents = newtoc.html.body.contents
	else:
		e.contents = newtoc.contents

	for d in soup.findAll('time'):
		curTime = datetime.utcnow()
		d['datetime']=curTime.strftime('%Y-%m-%d %H:%M:00Z')
		d.clear()
		d.append( curTime.strftime('%H:%M, %B %d, %Y') )
	
	convertDesc(soup)
	convertFields(soup)
	convertJson(soup)
	
	open('index.html', 'w').write(soupTab(soup, 'utf-8'))

if __name__ == "__main__":
    main()
