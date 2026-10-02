import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"

function App() {
  return (
    <div className="min-h-screen bg-background p-8">
      <h1 className="text-2xl font-semibold mb-6">Ecommerce Storefront (demo)</h1>
      <Card className="max-w-sm">
        <CardHeader>
          <CardTitle>Cây thuỷ sinh Anubias</CardTitle>
          <CardDescription>Cây dễ trồng, phù hợp người mới</CardDescription>
        </CardHeader>
        <CardContent>
          <p className="text-lg font-medium">45.000đ</p>
        </CardContent>
        <CardFooter>
          <Button>Thêm vào giỏ</Button>
        </CardFooter>
      </Card>
    </div>
  )
}

export default App
