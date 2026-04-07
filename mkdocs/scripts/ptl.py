# Prepare Tasks List summary
#
# Takes a hierarchy of tasks with statuses, and calculates the completion
# percentage of each task. Useful to generate plots.
#
# 1. Download the task list database from Notion as a CSV file called all.csv,
# 2. Import to Google sheet called `Task list progress`, on tab Raw.
# 3. Export tab Filtered to filtered.csv
#    Fields must be: Name, Status, Label, ID, Parent ID
# 4. Run this script
# 5. Copy stdout to the `Task list chart.ods` to generate plot.

import math
import pandas as pd

def iterate_count(id):
	prog = 0
	if id in ch:
		num_total = 0
		num_done = 0
		for c in ch[id]:
			num_total = num_total + 1
			num_done = num_done + iterate_count(c)
		prog = num_done / num_total
	else:
		prog = 1 if status[id] == 'Done' else 0
	percentage[id] = prog
	return prog

def iterate_print(id, indent):
	if id in ch:
		print("--" * (indent - 1) + ("->" if indent > 0 else ""), names[id], ",", percentage[id])
		for c in ch[id]:
			iterate_print(c, indent + 1)

d = pd.read_csv('filtered.csv')

ch = {}
names = {}
status = {}
percentage = {}
for v in d.values:
	id = v[3]
	if not math.isnan(v[4]):
		pid = int(v[4])
	else:
		pid = -1
	if pid not in ch:
		ch[pid] = []
	ch[pid].append(id)
	names[id] = v[0]
	status[id] = v[1]
	percentage[id] = 0

for v in d.values:
	if not math.isnan(v[4]):
		continue
	iterate_count(v[3])

for v in d.values:
	if not math.isnan(v[4]):
		continue
	iterate_print(v[3], 0)
