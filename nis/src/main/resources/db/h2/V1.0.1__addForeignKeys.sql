ALTER TABLE public.multisigtransactions ADD FOREIGN KEY (transferid)  REFERENCES transfers(id);
ALTER TABLE public.multisigtransactions ADD FOREIGN KEY (importancetransferid)  REFERENCES importancetransfers(id);
ALTER TABLE public.multisigtransactions ADD FOREIGN KEY (multisigsignermodificationid)  REFERENCES multisigsignermodifications(id);
