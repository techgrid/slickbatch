import uuidv4 from "uuid/v4";

export class AwsBatchMockService {
    public static jobDetails = {};
    public static jobDetailSummaries = {};
    public account = String(Math.floor(Math.random() * 999999999) + 100000000);

    public submit(request: any): any {
        return this.jobQueueArn("blah");
    }

    private jobQueueArn(queue: string): string {
        return `arn:aws:batch:us-east-1:${this.account}:job-queue/${queue}`;
    }

    private jobDefinitionArn(definition: string): string {
        return `arn:aws:batch:us-east-1:${this.account}:job-definition/${definition}`;
    }

    private logStreamName(definition: string): string {
        return `${definition}/default/${uuidv4()}`;
    }

    private containerInstanceArn(): string {
        return `arn:aws:batch:us-east-1:${this.account}:job-definition/${uuidv4()}`;
    }

    private containerTaskArn(): string {
        return `arn:aws:batch:us-east-1:${this.account}:task/${uuidv4()}`;
    }

    private containerImage(): string {
        return `${this.account}.dkr.ecr.us-east-1.amazonaws.com/user/test:GCC9-nexus-xpress-cpp`;
    }
}
