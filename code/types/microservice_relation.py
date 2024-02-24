class MicroserviceRelation:
    def __init__(self, source_microservice: str, source_class: str, target_microservice,
                 target_class: str):
        self.source_microservice = source_microservice
        self.source_class = source_class
        self.target_microservice = target_microservice
        self.target_class = target_class

    def __str__(self):
        return f"{self.source_microservice}.{self.source_class} -> {self.target_microservice}.{self.target_class}"
