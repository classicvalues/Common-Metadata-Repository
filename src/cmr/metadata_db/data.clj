(ns cmr.metadata-db.data)
  
(defprotocol ConceptStore
  "Functions for saving and retrieving concepts"
  (get-concept-id
    [db concept-type provider-id native-id]
    "Return a distinct identifier for the given arguments.")

  (get-concept
    [db concept-id, revision-id]
    "Gets a version of a concept with a given id")
  
  (get-concepts
    [db concept-id-revision-id-tuples]
    "Get multiple concepts at once by specifying a list of
    tuples holding concept-id/revision-id") 
    
  (save-concept
    [db concept]
    "Saves a concept and returns the revision id. If the concept already 
    exists then a new revision will be created. If a revision-id is 
    included and it is not valid, e.g. the revision already exists, 
    then an exception is thrown."))