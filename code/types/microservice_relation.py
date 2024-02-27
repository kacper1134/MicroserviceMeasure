class MicroserviceRelation:
    def __init__(self, source_microservice: str, source_class: str, target_microservice,
                 target_class: str):
        self.source_microservice = source_microservice
        self.source_class = source_class
        self.target_microservice = target_microservice
        self.target_class = target_class

    def __str__(self):
        return f"{self.source_microservice}.{self.source_class} -> {self.target_microservice}.{self.target_class}"

    def __eq__(self, other):
        return (isinstance(other, MicroserviceRelation) and
                self.source_microservice == other.source_microservice and
                self.source_class == other.source_class and
                self.target_microservice == other.target_microservice and
                self.target_class == other.target_class)

    def __hash__(self):
        return hash((self.source_microservice, self.source_class, self.target_microservice, self.target_class))