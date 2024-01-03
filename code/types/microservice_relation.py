class MicroserviceRelation:
    def __init__(self, source_microservice: str, source_class: str, source_method: str, target_microservice,
                 target_class: str, target_method: str):
        self.source_microservice = source_microservice
        self.source_class = source_class
        self.source_method = source_method
        self.target_microservice = target_microservice
        self.target_class = target_class
        self.target_method = target_method

    def __str__(self):
        return f"{self.source_microservice}.{self.source_class}.{self.source_method} -> {self.target_microservice}.{self.target_class}.{self.target_method}"
